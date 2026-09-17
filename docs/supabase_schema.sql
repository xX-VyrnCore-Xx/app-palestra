-- Palestra app schema for Supabase (Postgres)
-- Run this in the Supabase SQL editor. Assumes auth.users is managed by Supabase Auth.

create table if not exists public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    email text not null,
    full_name text not null,
    role text not null check (role in ('PT', 'ALLIEVO')),
    pt_id uuid references public.profiles (id) on delete set null,
    created_at timestamptz not null default now()
);

create table if not exists public.exercises (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    muscle_group text not null,
    equipment text,
    notes text,
    created_by_user_id uuid references public.profiles (id) on delete set null,
    is_custom boolean not null default false
);

create table if not exists public.workout_plans (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    description text,
    created_by_pt_id uuid not null references public.profiles (id) on delete cascade,
    assigned_to_user_id uuid not null references public.profiles (id) on delete cascade,
    created_at timestamptz not null default now()
);

create table if not exists public.plan_exercises (
    id uuid primary key default gen_random_uuid(),
    plan_id uuid not null references public.workout_plans (id) on delete cascade,
    exercise_id uuid not null references public.exercises (id) on delete cascade,
    order_index int not null default 0,
    target_sets int not null default 3,
    target_reps int not null default 10,
    target_weight_kg numeric,
    rest_seconds int not null default 90
);

create table if not exists public.workout_sessions (
    id uuid primary key default gen_random_uuid(),
    plan_id uuid references public.workout_plans (id) on delete set null,
    user_id uuid not null references public.profiles (id) on delete cascade,
    started_at timestamptz not null default now(),
    ended_at timestamptz,
    notes text
);

create table if not exists public.set_entries (
    id uuid primary key default gen_random_uuid(),
    session_id uuid not null references public.workout_sessions (id) on delete cascade,
    exercise_id uuid not null references public.exercises (id) on delete cascade,
    set_number int not null,
    reps int not null,
    weight_kg numeric not null,
    rpe numeric,
    completed_at timestamptz not null default now()
);

create table if not exists public.body_metrics (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles (id) on delete cascade,
    date timestamptz not null default now(),
    weight_kg numeric,
    body_fat_percent numeric,
    chest_cm numeric,
    waist_cm numeric,
    hips_cm numeric,
    arm_cm numeric,
    thigh_cm numeric,
    notes text
);

-- Row Level Security --------------------------------------------------------

alter table public.profiles enable row level security;
alter table public.exercises enable row level security;
alter table public.workout_plans enable row level security;
alter table public.plan_exercises enable row level security;
alter table public.workout_sessions enable row level security;
alter table public.set_entries enable row level security;
alter table public.body_metrics enable row level security;

-- profiles: a user can read/update their own row; a PT can read their clients' rows.
create policy "profiles_self_select" on public.profiles
    for select using (auth.uid() = id or auth.uid() = pt_id);
create policy "profiles_self_upsert" on public.profiles
    for insert with check (auth.uid() = id);
create policy "profiles_self_update" on public.profiles
    for update using (auth.uid() = id);

-- exercises: readable by everyone signed in; writable by the creator.
create policy "exercises_select" on public.exercises
    for select using (auth.role() = 'authenticated');
create policy "exercises_insert" on public.exercises
    for insert with check (auth.uid() = created_by_user_id or created_by_user_id is null);

-- workout_plans: PT manages plans they created; assigned client can read theirs.
create policy "plans_select" on public.workout_plans
    for select using (auth.uid() = created_by_pt_id or auth.uid() = assigned_to_user_id);
create policy "plans_insert" on public.workout_plans
    for insert with check (auth.uid() = created_by_pt_id);
create policy "plans_update" on public.workout_plans
    for update using (auth.uid() = created_by_pt_id);

-- plan_exercises: visible/writable by whoever can see the parent plan.
create policy "plan_exercises_select" on public.plan_exercises
    for select using (
        exists (
            select 1 from public.workout_plans p
            where p.id = plan_id and (p.created_by_pt_id = auth.uid() or p.assigned_to_user_id = auth.uid())
        )
    );
create policy "plan_exercises_write" on public.plan_exercises
    for insert with check (
        exists (select 1 from public.workout_plans p where p.id = plan_id and p.created_by_pt_id = auth.uid())
    );

-- workout_sessions: owned by the training user; their PT can read (via join on profiles.pt_id).
create policy "sessions_select" on public.workout_sessions
    for select using (
        auth.uid() = user_id or
        exists (select 1 from public.profiles pr where pr.id = user_id and pr.pt_id = auth.uid())
    );
create policy "sessions_write" on public.workout_sessions
    for insert with check (auth.uid() = user_id);
create policy "sessions_update" on public.workout_sessions
    for update using (auth.uid() = user_id);

-- set_entries: same visibility as the parent session.
create policy "set_entries_select" on public.set_entries
    for select using (
        exists (
            select 1 from public.workout_sessions s
            where s.id = session_id and (
                s.user_id = auth.uid() or
                exists (select 1 from public.profiles pr where pr.id = s.user_id and pr.pt_id = auth.uid())
            )
        )
    );
create policy "set_entries_write" on public.set_entries
    for insert with check (
        exists (select 1 from public.workout_sessions s where s.id = session_id and s.user_id = auth.uid())
    );

-- body_metrics: owned by the user; their PT can read.
create policy "body_metrics_select" on public.body_metrics
    for select using (
        auth.uid() = user_id or
        exists (select 1 from public.profiles pr where pr.id = user_id and pr.pt_id = auth.uid())
    );
create policy "body_metrics_write" on public.body_metrics
    for insert with check (auth.uid() = user_id);
