-- Per-anonymous-user daily quotas for the OpenAI-backed Object Detective function.
-- The Edge Function calls this RPC with the caller's Supabase JWT. No client can
-- read or write the usage table directly.
create table if not exists public.object_detective_usage (
  user_id uuid not null references auth.users(id) on delete cascade,
  usage_date date not null,
  analyses integer not null default 0 check (analyses >= 0),
  chat_messages integer not null default 0 check (chat_messages >= 0),
  primary key (user_id, usage_date)
);

alter table public.object_detective_usage enable row level security;
revoke all on table public.object_detective_usage from public, anon, authenticated;
create index if not exists object_detective_usage_date_idx
  on public.object_detective_usage (usage_date);

create or replace function public.consume_object_detective_quota(p_kind text)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_today date := (now() at time zone 'utc')::date;
begin
  if v_user_id is null or p_kind not in ('analysis', 'chat') then
    return false;
  end if;

  insert into public.object_detective_usage (user_id, usage_date)
  values (v_user_id, v_today)
  on conflict (user_id, usage_date) do nothing;

  if p_kind = 'analysis' then
    update public.object_detective_usage
       set analyses = analyses + 1
     where user_id = v_user_id
       and usage_date = v_today
       and analyses < 30;
  else
    update public.object_detective_usage
       set chat_messages = chat_messages + 1
     where user_id = v_user_id
       and usage_date = v_today
       and chat_messages < 120;
  end if;

  delete from public.object_detective_usage
   where usage_date < v_today - 2;

  return found;
end;
$$;

revoke all on function public.consume_object_detective_quota(text) from public, anon;
grant execute on function public.consume_object_detective_quota(text) to authenticated;
