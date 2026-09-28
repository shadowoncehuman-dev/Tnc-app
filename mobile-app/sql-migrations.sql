-- Create user profiles table - stores user name, device info, and activity tracking
create table if not exists public.user_profiles (
  id uuid references auth.users on delete cascade not null primary key,
  name text not null,
  device_id text,
  ip_address text,
  network_info text,
  total_opens integer default 0,
  daily_opens integer default 0,
  weekly_opens integer default 0,
  last_open_at timestamptz default now(),
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

-- Create app statistics table - tracks overall app metrics
create table if not exists public.app_stats (
  id uuid references auth.users on delete cascade not null primary key,
  total_downloads integer default 0,
  total_opens integer default 0,
  daily_open_count integer default 0,
  weekly_open_count integer default 0,
  last_open_date date default (current_date),
  updated_at timestamptz default now()
);

-- Create blocked users table - admin can block devices/IPs
create table if not exists public.blocked_users (
  id uuid references auth.users on delete cascade not null primary key,
  blocked_device_id text,
  blocked_ip text,
  blocked_by_admin uuid references auth.users on delete cascade,
  block_reason text,
  blocked_at timestamptz default now(),
  is_active boolean default true
);

-- Enable Row Level Security
alter table public.user_profiles enable row level security;
alter table public.app_stats enable row level security;
alter table public.blocked_users enable row level security;

-- Create policies for user_profiles
create policy "Users can read own profile" on public.user_profiles
  for select using (auth.uid() = id);

create policy "Admins can manage profiles" on public.user_profiles
  for all using (exists(select 1 from auth.users where email = 'admin@tnc.com'));

-- Create policies for app_stats
create policy "Users can read own stats" on public.app_stats
  for select using (auth.uid() = id);

create policy "Admins can manage stats" on public.app_stats
  for all using (exists(select 1 from auth.users where email = 'admin@tnc.com'));

-- Create policies for blocked_users
create policy "Users can read own blocks" on public.blocked_users
  for select using (auth.uid() = id or blocked_by_admin = auth.uid());

create policy "Admins can manage blocks" on public.blocked_users
  for all using (exists(select 1 from auth.users where email = 'admin@tnc.com'));

-- Create index for faster lookups
create index idx_user_profiles_device_id on public.user_profiles(device_id);
create index idx_user_profiles_name on public.user_profiles(name);
create index idx_blocked_users_device_ip on public.blocked_users(blocked_device_id, blocked_ip);

-- ==========================================
-- Helper functions for incrementing counters
-- ==========================================

-- Function to increment total opens count
create or replace function increment_total_opens(profile_id uuid)
returns integer language sql security definer as $$
  update public.user_profiles set total_opens = total_opens + 1, last_open_at = now() where id = profile_id returning total_opens;
$$;

-- Function to increment daily opens count
create or replace function increment_daily_opens(profile_id uuid)
returns integer language sql security definer as $$
  update public.user_profiles set daily_opens = daily_opens + 1 where id = profile_id returning daily_opens;
$$;

-- Function to increment weekly opens count
create or replace function increment_weekly_opens(profile_id uuid)
returns integer language sql security definer as $$
  update public.user_profiles set weekly_opens = weekly_opens + 1 where id = profile_id returning weekly_opens;
$$;