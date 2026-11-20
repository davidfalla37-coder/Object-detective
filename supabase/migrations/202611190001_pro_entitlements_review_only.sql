-- Object Detective Pro billing foundation.
-- REVIEW ONLY: do not apply until Supabase Auth identity and backend verification are implemented.
-- All writes are server-side only. Never let a client set its own Pro entitlement.

create table if not exists public.pro_entitlements (
  user_id uuid primary key references auth.users(id) on delete cascade,
  tier text not null default 'free' check (tier in ('free', 'pro')),
  product_id text,
  purchase_state text not null default 'none'
    check (purchase_state in ('none', 'pending', 'purchased', 'grace_period', 'on_hold', 'paused', 'expired', 'revoked')),
  valid_until timestamptz,
  updated_at timestamptz not null default now(),
  constraint pro_entitlement_consistency check (
    (tier = 'free') or
    (tier = 'pro' and purchase_state in ('purchased', 'grace_period') and valid_until is not null)
  )
);

-- Store encrypted purchase tokens only. Encryption/decryption must happen in a trusted
-- backend using a key held in Supabase Function secrets, never in the app or repository.
create table if not exists public.pro_purchase_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  package_name text not null,
  product_id text not null,
  token_sha256 text not null unique,
  token_ciphertext text not null,
  purchase_state text not null
    check (purchase_state in ('pending', 'purchased', 'grace_period', 'on_hold', 'paused', 'expired', 'revoked')),
  order_id text,
  expiry_time timestamptz,
  last_verified_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists pro_purchase_tokens_user_id_idx
  on public.pro_purchase_tokens(user_id);

alter table public.pro_entitlements enable row level security;
alter table public.pro_purchase_tokens enable row level security;

-- Users may read their own entitlement status, but cannot create or modify it.
drop policy if exists "Users can read own Pro entitlement" on public.pro_entitlements;
create policy "Users can read own Pro entitlement"
  on public.pro_entitlements for select
  to authenticated
  using (auth.uid() = user_id);

-- No client policies on pro_purchase_tokens: purchase tokens and ciphertext are backend-only.
-- Do not grant anon/authenticated insert, update, or delete privileges.
revoke all on public.pro_purchase_tokens from anon, authenticated;
revoke insert, update, delete on public.pro_entitlements from anon, authenticated;

-- A trusted Edge Function must update entitlements only after verifying the purchase
-- with Google Play Developer API. Clients must never supply trusted purchase_state/tier.
