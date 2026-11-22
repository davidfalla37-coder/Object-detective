const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "content-type, authorization, apikey, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

async function fetchJson(url: string, userAgent: string): Promise<{ response: Response; data: any }> {
  const response = await fetch(url, {
    headers: { "User-Agent": userAgent, "Accept": "application/json" },
    redirect: "follow",
    signal: AbortSignal.timeout(7000),
  });
  const data = response.ok ? await response.json().catch(() => null) : null;
  return { response, data };
}

function expandUpcE(value: string): string | null {
  if (!/^\d{8}$/.test(value)) return null;
  const ns = value[0], d = value.slice(1, 7), check = value[7];
  const a = d[0], b = d[1], c = d[2], e = d[3], f = d[4], last = d[5];
  let body = "";
  if ("012".includes(last)) body = ns + a + b + last + "0000" + c + e + f;
  else if (last === "3") body = ns + a + b + c + "00000" + e + f;
  else if (last === "4") body = ns + a + b + c + e + "00000" + f;
  else body = ns + a + b + c + e + f + "0000" + last;
  return body + check;
}

function extractGtin(raw: string): string | null {
  const value = raw.trim();
  // GS1 Digital Link URL: /01/{GTIN}; also accept parenthesised and scanner AIM prefixes.
  try {
    if (/^https?:\/\//i.test(value)) {
      const parts = new URL(value).pathname.split("/").filter(Boolean);
      for (let i = 0; i < parts.length - 1; i++) {
        if (parts[i] === "01" && /^\d{14}$/.test(parts[i + 1])) return parts[i + 1];
      }
    }
  } catch { /* Continue with plain-text parsing. */ }
  const cleaned = value.replace(/^\][A-Za-z]\d/, "").replace(/\s/g, "");
  const parenthesised = cleaned.match(/\(01\)(\d{14})/);
  if (parenthesised) return parenthesised[1];
  const gs1 = cleaned.match(/(?:^|[^0-9])01(\d{14})(?:21|10|17|$)/);
  if (gs1) return gs1[1];
  if (/^\d{8}$|^\d{12}$|^\d{13}$|^\d{14}$/.test(cleaned)) return cleaned;
  return null;
}

async function lookupBook(isbn: string) {
  const googleUrl = new URL("https://www.googleapis.com/books/v1/volumes");
  googleUrl.searchParams.set("q", "isbn:" + isbn);
  googleUrl.searchParams.set("maxResults", "5");
  const google = await fetchJson(googleUrl.toString(), "ObjectDetective/1.20 (book barcode lookup)");
  const items = Array.isArray(google.data?.items) ? google.data.items : [];
  for (const item of items) {
    const info = item.volumeInfo || {};
    const identifiers = Array.isArray(info.industryIdentifiers) ? info.industryIdentifiers : [];
    const exact = identifiers.some((entry: any) => String(entry.identifier || "").replace(/[-\s]/g, "") === isbn);
    if (exact && info.title) {
      return {
        found: true,
        product: {
          name: String(info.title),
          brand: String(info.publisher || ""),
          type: "Book",
          quantity: [info.publishedDate, Array.isArray(info.authors) ? info.authors.join(", ") : ""].filter(Boolean).join(" · "),
        },
        source: "Google Books",
        sourceUrl: "https://books.google.com/",
        disclaimer: "Book metadata matched the exact ISBN. Edition details should still be checked.",
      };
    }
  }

  const openLibrary = await fetchJson(
    "https://openlibrary.org/isbn/" + encodeURIComponent(isbn) + ".json",
    "ObjectDetective/1.20 (book barcode lookup)",
  );
  if (openLibrary.response.ok && openLibrary.data?.title) {
    return {
      found: true,
      product: {
        name: String(openLibrary.data.title),
        brand: Array.isArray(openLibrary.data.publishers) ? openLibrary.data.publishers.join(", ") : "",
        type: "Book",
        quantity: String(openLibrary.data.publish_date || ""),
      },
      source: "Open Library",
      sourceUrl: "https://openlibrary.org/",
      disclaimer: "Book metadata matched the ISBN record. Edition details should still be checked.",
    };
  }
  return null;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Use POST for barcode lookup." }, 405);

  try {
    const body = await req.json().catch(() => null) as { code?: unknown; format?: unknown } | null;
    const rawCode = typeof body?.code === "string" ? body.code.trim() : "";
    const format = typeof body?.format === "string" ? body.format : "";
    if (!rawCode || rawCode.length > 2048) {
      return json({ found: false, message: "No readable code was supplied, or the scanned content was too long." }, 400);
    }

    // UPC-E is an 8-digit compressed UPC, not an EAN-8 GTIN. Expand it before lookup.
    const digitsOnly = rawCode.replace(/\s/g, "");
    const upcA = format.toUpperCase().includes("UPC-E") ? expandUpcE(digitsOnly) : null;
    const gtin = upcA || extractGtin(rawCode);

    // ISBN-13 book barcodes use the 978/979 prefixes. ISBN-10 is accepted if a scanner returns it.
    const isbn = gtin && /^97[89]\d{10}$/.test(gtin) ? gtin :
      (/^(?:\d{9}[\dX])$/.test(digitsOnly) ? digitsOnly : null);
    if (isbn) {
      const book = await lookupBook(isbn);
      if (book) return json(book);
    }

    if (!gtin) {
      return json({
        found: false,
        code: rawCode,
        format,
        message: "The code was successfully read, but it is not a recognised retail GTIN or ISBN. It may be a serial number, part number, URL or internal identifier; use the object's label and model details to identify it.",
      });
    }

    // Open Facts' product_type=all endpoint checks Food, Beauty, Pet Food and general Products Facts.
    // It follows redirects to the correct database when a record belongs to another catalogue.
    const url = new URL(`https://world.openfoodfacts.org/api/v3/product/${encodeURIComponent(gtin)}`);
    url.searchParams.set("product_type", "all");
    url.searchParams.set("fields", "code,product_name,product_name_en,brands,product_type,quantity,categories,image_front_url");
    const { response, data } = await fetchJson(url.toString(), "ObjectDetective/1.20 (https://play.google.com/apps/testing/com.objectdetective.app)");
    if (!response.ok) {
      console.error("Open Facts lookup response:", response.status);
      return json({ found: false, code: gtin, message: "The barcode was read, but the product catalogues are temporarily unavailable. Please try again shortly." }, 502);
    }

    const productFound = data?.status === 1 || data?.status === "success" ||
      data?.status === "success_with_errors" || data?.result?.id === "product_found";
    const product = data?.product;
    if (productFound && product) {
      const name = String(product.product_name || product.product_name_en || "").trim();
      const brand = String(product.brands || "").split(",")[0].trim();
      const type = String(product.product_type || product.categories || "").trim();
      const quantity = String(product.quantity || "").trim();
      if (name || brand || type) {
        const host = new URL(response.url).hostname;
        const source = ({
          "world.openfoodfacts.org": "Open Food Facts",
          "world.openproductsfacts.org": "Open Products Facts",
          "world.openbeautyfacts.org": "Open Beauty Facts",
          "world.openpetfoodfacts.org": "Open Pet Food Facts",
        } as Record<string, string>)[host] || "Open Facts database";
        return json({
          found: true,
          product: { name: name || "Name not supplied", brand, type, quantity },
          source,
          sourceUrl: new URL(response.url).origin,
          disclaimer: "Catalogue information may be incomplete or user-contributed. Compare the brand, model and packaging before relying on the match.",
        });
      }
    }

    // Optional commercial catalogue adapter. It stays dormant unless an API key
    // is explicitly configured as a server-side Supabase secret; no paid calls by default.
    const commercialKey = Deno.env.get("BARCODE_LOOKUP_API_KEY");
    if (commercialKey) {
      const commercialUrl = new URL("https://api.barcodelookup.com/v3/products");
      commercialUrl.searchParams.set("barcode", gtin);
      commercialUrl.searchParams.set("formatted", "y");
      commercialUrl.searchParams.set("key", commercialKey);
      const commercial = await fetchJson(commercialUrl.toString(), "ObjectDetective/1.20 (barcode catalogue)");
      const first = Array.isArray(commercial.data?.products) ? commercial.data.products[0] : null;
      if (commercial.response.ok && first) {
        const name = String(first.title || first.product_name || first.name || "").trim();
        const brand = String(first.brand || first.manufacturer || "").trim();
        const model = String(first.model || first.mpn || "").trim();
        const category = String(first.category || "").trim();
        if (name || brand || model) {
          return json({
            found: true,
            product: { name: name || model, brand, type: [category, model && model !== name ? "Model " + model : ""].filter(Boolean).join(" · "), quantity: "" },
            source: "Barcode Lookup",
            sourceUrl: "https://www.barcodelookup.com/",
            disclaimer: "Commercial catalogue match. Compare the model and product details against the item before relying on it.",
          });
        }
      }
    }

    return json({
      found: false,
      code: gtin,
      format,
      message: "The barcode was read, but no product record was found in the connected open catalogues (Food, Beauty, Pet Food and general Products Facts). A wider commercial barcode catalogue can be enabled separately; manufacturer model/serial labels can also be checked using Object Detective's photo and text identification.",
    });
  } catch (error) {
    console.error("Barcode lookup failed:", error instanceof Error ? error.message : error);
    return json({ error: "The product lookup could not be completed. Please try again." }, 500);
  }
});
