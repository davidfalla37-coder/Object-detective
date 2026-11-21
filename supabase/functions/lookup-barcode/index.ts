const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "content-type, authorization, apikey, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Use POST for barcode lookup." }, 405);

  try {
    const body = await req.json().catch(() => null) as { code?: unknown } | null;
    const code = typeof body?.code === "string" ? body.code.trim() : "";
    // GTIN-8/12/13/14 only. Other barcode formats are readable by the scanner
    // but do not identify a retail product through this catalogue endpoint.
    if (!/^\d{8}$|^\d{12}$|^\d{13}$|^\d{14}$/.test(code)) {
      return json({
        found: false,
        message: "This code was read, but it is not a supported retail product barcode (GTIN-8/12/13/14).",
      }, 200);
    }

    const url = new URL(`https://world.openfoodfacts.org/api/v3.6/product/${encodeURIComponent(code)}.json`);
    url.searchParams.set("product_type", "all");
    url.searchParams.set("fields", "code,product_name,product_name_en,brands,product_type,quantity,categories,image_front_url");

    const response = await fetch(url, {
      method: "GET",
      headers: {
        "User-Agent": "ObjectDetective/1.19 (https://play.google.com/apps/testing/com.objectdetective.app)",
        "Accept": "application/json",
      },
      redirect: "follow",
      signal: AbortSignal.timeout(9000),
    });
    if (!response.ok) {
      console.error("Barcode catalogue response:", response.status);
      return json({ found: false, message: "The product catalogue is temporarily unavailable. Please try again shortly." }, 502);
    }

    const payload = await response.json() as {
      status?: number;
      product?: {
        product_name?: string;
        product_name_en?: string;
        brands?: string;
        product_type?: string;
        quantity?: string;
        categories?: string;
      };
    };
    if (payload.status !== 1 || !payload.product) {
      return json({
        found: false,
        message: "No matching product was found in the connected open product catalogue. The barcode was read, but this item has not been identified.",
      });
    }

    const product = payload.product;
    const name = (product.product_name || product.product_name_en || "").trim();
    const brand = (product.brands || "").split(",")[0].trim();
    const type = (product.product_type || product.categories || "").trim();
    const quantity = (product.quantity || "").trim();
    if (!name && !brand && !type) {
      return json({
        found: false,
        message: "A record exists for this barcode, but it does not contain enough product details to identify the item reliably.",
      });
    }

    return json({
      found: true,
      product: {
        name: name || "Name not supplied",
        brand: brand || "",
        type: type || "",
        quantity: quantity || "",
      },
      source: new URL(response.url).hostname.replace(/^world\./, "").replace(/\.org$/, "").replace(/-/g, " ").replace(/\b\w/g, (letter) => letter.toUpperCase()),
      sourceUrl: new URL(response.url).origin,
      disclaimer: "Catalogue information may be incomplete or user-contributed. Verify the packaging and model.",
    });
  } catch (error) {
    console.error("Barcode lookup failed:", error instanceof Error ? error.message : error);
    return json({ error: "The product lookup could not be completed. Please try again." }, 500);
  }
});
