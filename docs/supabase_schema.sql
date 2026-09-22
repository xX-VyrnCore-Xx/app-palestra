-- Palestra app schema for Supabase (Postgres)
-- Run this in the Supabase SQL editor. Assumes auth.users is managed by Supabase Auth.

create table if not exists public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    email text not null,
    full_name text not null,
    role text not null check (role in ('PT', 'ALLIEVO')),
    pt_id uuid references public.profiles (id) on delete set null,
    -- Infortuni/limitazioni fisiche impostate dal PT, mai visibili/scrivibili da altri PT.
    injuries text,
    -- Token FCM del dispositivo corrente, usato dalla Edge Function send-push per le notifiche.
    fcm_token text,
    -- Foto profilo scelta dall'utente, caricata nel bucket pubblico "avatars".
    avatar_url text,
    -- Breve biografia mostrata in testa al profilo (max 200 caratteri, lato app).
    bio text,
    -- Altezza in centimetri e peso in kg: precompilano le metriche corporee e i calcoli BMI.
    height_cm int,
    weight_kg numeric,
    -- Obiettivo dichiarato dall'utente in fase di onboarding o dalla sheet profilo
    -- (es. 'Perdere peso', 'Aumentare massa', ...).
    primary_goal text,
    -- Codice breve e condivisibile per collegare un allievo al PT senza incollare l'id grezzo.
    -- Solo i PT ne hanno uno, generato al bisogno lato app.
    invite_code text unique,
    created_at timestamptz not null default now()
);

-- Profilo dell'allievo compilato nella Welcome Page al primo accesso (esperienza, giorni di
-- allenamento, obiettivo, stile di vita, dolori/lesioni, alimentazione). Leggibile/scrivibile dal
-- proprietario; il proprio PT può leggerlo (mai scriverlo) per costruire una scheda su misura -
-- vedi la policy _pt_read sotto. L'assistente AI non lo legge mai: nessun altro punto della
-- codebase deve toccare questa tabella.
create table if not exists public.allievo_private_profiles (
    user_id uuid primary key references public.profiles (id) on delete cascade,
    experience_level text,
    training_days text,
    activity_level text,
    primary_goal text,
    pain_injuries text,
    nutrition text,
    lifestyle text,
    goals text,
    completed_onboarding boolean not null default false,
    updated_at timestamptz not null default now()
);

create table if not exists public.exercises (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    muscle_group text not null,
    equipment text,
    notes text,
    created_by_user_id uuid references public.profiles (id) on delete set null,
    is_custom boolean not null default false,
    -- Optional demonstrative image/GIF URL, shown wherever the exercise appears.
    image_url text
);

-- Structured multi-week programs (mesocicli): a PT sets a base plan, a week count and a
-- per-week load increment; each week is generated as its own row in workout_plans, tagged with
-- program_id/week_index below.
create table if not exists public.programs (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    created_by_pt_id uuid not null references public.profiles (id) on delete cascade,
    assigned_to_user_id uuid not null references public.profiles (id) on delete cascade,
    total_weeks int not null,
    weekly_increment_percent numeric not null default 0,
    start_at timestamptz not null default now()
);

create table if not exists public.workout_plans (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    description text,
    created_by_pt_id uuid not null references public.profiles (id) on delete cascade,
    assigned_to_user_id uuid not null references public.profiles (id) on delete cascade,
    created_at timestamptz not null default now(),
    category text,
    estimated_minutes int,
    -- Set when this plan is one week of a program above; null for a standalone plan.
    program_id uuid references public.programs (id) on delete cascade,
    week_index int
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

-- Realtime chat between a PT and their allievo.
create table if not exists public.messages (
    id uuid primary key default gen_random_uuid(),
    sender_id uuid not null references public.profiles (id),
    recipient_id uuid not null references public.profiles (id),
    content text not null,
    created_at timestamptz not null default now(),
    read_at timestamptz,
    -- Set together when a message carries a link/file/image instead of (or alongside) text.
    attachment_url text,
    attachment_name text,
    attachment_type text,
    -- Soft-delete: the row stays as a tombstone so the peer sees "messaggio eliminato"
    -- instead of a confusing gap in the conversation.
    is_deleted boolean not null default false
);

create index if not exists messages_conversation_idx
    on public.messages (least(sender_id, recipient_id), greatest(sender_id, recipient_id), created_at);

-- Private PT notes about a client, visible only to the PT who wrote them.
create table if not exists public.pt_notes (
    id uuid primary key default gen_random_uuid(),
    pt_id uuid not null references public.profiles (id),
    client_id uuid not null references public.profiles (id),
    content text not null default '',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists pt_notes_pt_client_idx on public.pt_notes (pt_id, client_id);

-- AI assistant: per-user conversation history with the ai-chat Edge Function (separate PT/Allievo
-- system prompts chosen server-side; see supabase/functions/ai-chat). Each user only ever sees
-- their own messages.
create table if not exists public.ai_messages (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles (id) on delete cascade,
    role text not null check (role in ('user', 'assistant')),
    content text not null,
    created_at timestamptz not null default now()
);

create index if not exists ai_messages_user_idx on public.ai_messages (user_id, created_at);

-- Plotone feed: a lightweight activity feed, auto-posted when an allievo finishes a workout,
-- visible to every allievo sharing the same PT (and to that PT). display_name is captured at
-- insert time (first name only, matching get_weekly_ranking's privacy bar) so reads never need
-- a join back to profiles.
create table if not exists public.plotone_feed_posts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles (id) on delete cascade,
    pt_id uuid not null references public.profiles (id) on delete cascade,
    display_name text not null,
    message text not null,
    created_at timestamptz not null default now()
);

create index if not exists plotone_feed_posts_pt_idx on public.plotone_feed_posts (pt_id, created_at);

-- Account-wide NVIDIA NIM sliding-window rate limit (the API key is capped at 40 requests/minute
-- across every user); only the ai-chat Edge Function (service role) reads/writes this table.
create table if not exists public.ai_rate_limit_events (
    id bigint generated always as identity primary key,
    created_at timestamptz not null default now()
);

-- Row Level Security --------------------------------------------------------

-- Migrazione incrementale per installazioni esistenti: colonne profilo estese + difficoltà
-- esercizi aggiunte in seguito al rilascio iniziale. Su un database nuovo le create table
-- sopra le includono già, quindi questi alter vanno eseguiti solo su schemi preesistenti.
alter table public.profiles add column if not exists bio text;
alter table public.profiles add column if not exists height_cm int;
alter table public.profiles add column if not exists weight_kg numeric;
alter table public.profiles add column if not exists primary_goal text;
alter table public.exercises add column if not exists difficulty text;
alter table public.profiles add column if not exists invite_code text unique;

alter table public.profiles enable row level security;
alter table public.allievo_private_profiles enable row level security;
alter table public.exercises enable row level security;
alter table public.workout_plans enable row level security;
alter table public.ai_messages enable row level security;
alter table public.ai_rate_limit_events enable row level security;
alter table public.plan_exercises enable row level security;
alter table public.workout_sessions enable row level security;
alter table public.set_entries enable row level security;
alter table public.body_metrics enable row level security;
alter table public.messages enable row level security;
alter table public.pt_notes enable row level security;
alter table public.plotone_feed_posts enable row level security;
alter table public.programs enable row level security;

-- profiles: a user can read/update their own row; a PT can read their clients' rows.
create policy "profiles_self_select" on public.profiles
    for select using (auth.uid() = id or auth.uid() = pt_id);
create policy "profiles_self_upsert" on public.profiles
    for insert with check (auth.uid() = id);
create policy "profiles_self_update" on public.profiles
    for update using (auth.uid() = id);
-- a PT can also update their own clients' rows (used to record injuries/limitations).
create policy "profiles_pt_update_client" on public.profiles
    for update using (auth.uid() = pt_id);

-- allievo_private_profiles: full read/write for the owner, read-only for their own PT (so a
-- new client's answers can inform the plan the PT builds them) - see the table comment above.
create policy "allievo_private_profiles_owner_only" on public.allievo_private_profiles
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "allievo_private_profiles_pt_read" on public.allievo_private_profiles
    for select using (
        exists (
            select 1 from public.profiles pr
            where pr.id = allievo_private_profiles.user_id and pr.pt_id = auth.uid()
        )
    );

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
-- Update rights cover both the owning PT (editing a scheda) and the assigned allievo (the
-- proactive small progression nudge on target_reps/target_weight_kg after a clean session) -
-- without this, that client-side auto-bump would fail to sync back to Supabase.
create policy "plan_exercises_update" on public.plan_exercises
    for update using (
        exists (
            select 1 from public.workout_plans p
            where p.id = plan_id and (p.created_by_pt_id = auth.uid() or p.assigned_to_user_id = auth.uid())
        )
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

-- messages: readable/writable only by the two participants; a message may only be sent
-- between a PT and their own allievo (either direction).
create policy "messages_select_own_conversations" on public.messages
    for select using (auth.uid() = sender_id or auth.uid() = recipient_id);
create policy "messages_insert_own_conversations" on public.messages
    for insert with check (
        auth.uid() = sender_id
        and exists (
            select 1 from public.profiles p
            where p.id = sender_id
              and (p.pt_id = recipient_id or exists (
                  select 1 from public.profiles c where c.pt_id = sender_id and c.id = recipient_id
              ))
        )
    );
create policy "messages_update_own_read_at" on public.messages
    for update using (auth.uid() = recipient_id) with check (auth.uid() = recipient_id);

-- pt_notes: visible/writable only by the PT who owns the note.
create policy "pt_notes_owner_select" on public.pt_notes
    for select using (auth.uid() = pt_id);
create policy "pt_notes_owner_insert" on public.pt_notes
    for insert with check (auth.uid() = pt_id);
create policy "pt_notes_owner_update" on public.pt_notes
    for update using (auth.uid() = pt_id) with check (auth.uid() = pt_id);

-- programs: PT manages programs they created; assigned client can read theirs (same shape as
-- workout_plans' policies, since a program is just a container for a run of weekly plans).
create policy "programs_select" on public.programs
    for select using (auth.uid() = created_by_pt_id or auth.uid() = assigned_to_user_id);
create policy "programs_insert" on public.programs
    for insert with check (auth.uid() = created_by_pt_id);
create policy "programs_update" on public.programs
    for update using (auth.uid() = created_by_pt_id);

-- plotone_feed_posts: visible to the PT and to every allievo sharing that same PT; an allievo
-- may only post as themself.
create policy "plotone_feed_select" on public.plotone_feed_posts
    for select using (
        auth.uid() = pt_id or
        exists (select 1 from public.profiles pr where pr.id = auth.uid() and pr.pt_id = plotone_feed_posts.pt_id)
    );
create policy "plotone_feed_insert" on public.plotone_feed_posts
    for insert with check (auth.uid() = user_id);

-- ai_messages: visible/writable only by the user the conversation belongs to.
-- ai_rate_limit_events has no client policies at all: only the Edge Function's service-role
-- key (which bypasses RLS) reads/writes it.
create policy "ai_messages_owner_select" on public.ai_messages
    for select using (auth.uid() = user_id);
create policy "ai_messages_owner_insert" on public.ai_messages
    for insert with check (auth.uid() = user_id);

-- Realtime -------------------------------------------------------------------
-- Live updates (no rebuild/refresh needed): the app listens on these tables via Supabase
-- Realtime, so a PT-assigned/edited plan or a new body-metric entry appears on the other
-- device immediately instead of waiting for SyncManager's periodic pull. RLS above already
-- governs which rows Realtime delivers to each connected user.
alter publication supabase_realtime add table public.messages;
alter publication supabase_realtime add table public.workout_plans;
alter publication supabase_realtime add table public.plan_exercises;
alter publication supabase_realtime add table public.body_metrics;
alter publication supabase_realtime add table public.plotone_feed_posts;

-- Weekly ranking (PT and Allievo home screens) -------------------------------
-- SECURITY DEFINER so an allievo can see how they compare to peers of the same PT without
-- being granted broad RLS read access to other users' profiles/sessions - this function only
-- ever returns a first name and a workout count, scoped to same-PT peers via auth.uid().
create or replace function public.get_weekly_ranking()
returns table(display_name text, workouts_this_week bigint)
language sql
security definer
set search_path = public
as $$
  select
    split_part(p.full_name, ' ', 1) as display_name,
    count(ws.id) filter (
      where ws.ended_at is not null and ws.ended_at > now() - interval '7 days'
    ) as workouts_this_week
  from public.profiles p
  join public.profiles me on me.id = auth.uid()
  left join public.workout_sessions ws on ws.user_id = p.id
  where me.pt_id is not null and p.pt_id = me.pt_id
  group by p.id, p.full_name
  having count(ws.id) filter (
    where ws.ended_at is not null and ws.ended_at > now() - interval '7 days'
  ) > 0
  order by workouts_this_week desc
  limit 10;
$$;

revoke execute on function public.get_weekly_ranking() from anon;
revoke execute on function public.get_weekly_ranking() from public;
grant execute on function public.get_weekly_ranking() to authenticated;

-- Plan templates (PT's reusable exercise-list library) ------------------------
create table if not exists public.plan_templates (
    id uuid primary key,
    pt_id uuid not null references public.profiles (id) on delete cascade,
    name text not null,
    category text,
    created_at timestamptz not null default now()
);

create table if not exists public.plan_template_exercises (
    id uuid primary key,
    template_id uuid not null references public.plan_templates (id) on delete cascade,
    exercise_id uuid not null references public.exercises (id) on delete cascade,
    order_index int not null default 0,
    target_sets int not null,
    target_reps int not null,
    target_weight_kg numeric,
    rest_seconds int not null default 90,
    notes text
);

create index if not exists idx_plan_templates_pt_id on public.plan_templates (pt_id);
create index if not exists idx_plan_template_exercises_template_id on public.plan_template_exercises (template_id);
-- Covering index for the exercise_id FK (perf advisor finding).
create index if not exists idx_plan_template_exercises_exercise_id on public.plan_template_exercises (exercise_id);

alter table public.plan_templates enable row level security;
alter table public.plan_template_exercises enable row level security;

-- auth.uid() wrapped in (select ...) so RLS evaluates it once per query instead of once per row
-- (perf advisor finding).
create policy "plan_templates_select" on public.plan_templates
    for select using ((select auth.uid()) = pt_id);
create policy "plan_templates_insert" on public.plan_templates
    for insert with check ((select auth.uid()) = pt_id);
create policy "plan_templates_update" on public.plan_templates
    for update using ((select auth.uid()) = pt_id);
create policy "plan_templates_delete" on public.plan_templates
    for delete using ((select auth.uid()) = pt_id);

create policy "plan_template_exercises_select" on public.plan_template_exercises
    for select using (exists (select 1 from public.plan_templates t where t.id = template_id and t.pt_id = (select auth.uid())));
create policy "plan_template_exercises_insert" on public.plan_template_exercises
    for insert with check (exists (select 1 from public.plan_templates t where t.id = template_id and t.pt_id = (select auth.uid())));
create policy "plan_template_exercises_update" on public.plan_template_exercises
    for update using (exists (select 1 from public.plan_templates t where t.id = template_id and t.pt_id = (select auth.uid())));
create policy "plan_template_exercises_delete" on public.plan_template_exercises
    for delete using (exists (select 1 from public.plan_templates t where t.id = template_id and t.pt_id = (select auth.uid())));

-- Device push tokens -----------------------------------------------------------
-- profiles.fcm_token only ever held one device; this lets every device a user is signed into
-- (phone, tablet, ...) receive pushes, not just whichever last overwrote the single column.
create table if not exists public.device_tokens (
    user_id uuid not null references public.profiles (id) on delete cascade,
    fcm_token text not null,
    platform text not null default 'android',
    updated_at timestamptz not null default now(),
    primary key (user_id, fcm_token)
);

create index if not exists idx_device_tokens_user_id on public.device_tokens (user_id);

alter table public.device_tokens enable row level security;

create policy "device_tokens_select" on public.device_tokens
    for select using ((select auth.uid()) = user_id);
create policy "device_tokens_insert" on public.device_tokens
    for insert with check ((select auth.uid()) = user_id);
create policy "device_tokens_update" on public.device_tokens
    for update using ((select auth.uid()) = user_id);
create policy "device_tokens_delete" on public.device_tokens
    for delete using ((select auth.uid()) = user_id);

-- PT invite codes -------------------------------------------------------------
-- SECURITY DEFINER so any signed-in user can resolve a PT's short invite code to their id/name
-- (to link during registration or later from the profile screen) without being granted broad RLS
-- read access to other users' profiles - only ever returns id/name for an exact code match on a PT.
create or replace function public.resolve_pt_invite_code(code text)
returns table(id uuid, full_name text)
language sql
security definer
set search_path = public
as $$
  select p.id, p.full_name
  from public.profiles p
  where p.invite_code = upper(code) and p.role = 'PT'
  limit 1;
$$;

revoke execute on function public.resolve_pt_invite_code(text) from anon;
revoke execute on function public.resolve_pt_invite_code(text) from public;
grant execute on function public.resolve_pt_invite_code(text) to authenticated;

-- Chat attachments storage ---------------------------------------------------
-- Public bucket (object names are random UUIDs, so effectively unguessable) keeps
-- read access simple; write is restricted to the uploader's own "<user_id>/..."
-- folder - the path convention alone used to be documented but not enforced, so
-- any signed-in user could write into another user's folder.

insert into storage.buckets (id, name, public)
values ('chat-attachments', 'chat-attachments', true)
on conflict (id) do nothing;

create policy "chat_attachments_insert" on storage.objects
    for insert with check (
        bucket_id = 'chat-attachments'
        and auth.role() = 'authenticated'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );
create policy "chat_attachments_select" on storage.objects
    for select using (bucket_id = 'chat-attachments');

-- Profile avatars storage -----------------------------------------------------
-- Same shape as chat-attachments: public read, write scoped to the uploader's own
-- "<user_id>/..." folder so a user can only overwrite their own past uploads.

insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do nothing;

create policy "avatars_insert" on storage.objects
    for insert with check (
        bucket_id = 'avatars'
        and auth.role() = 'authenticated'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );
create policy "avatars_update" on storage.objects
    for update using (
        bucket_id = 'avatars'
        and auth.role() = 'authenticated'
        and (storage.foldername(name))[1] = (select auth.uid())::text
    );
create policy "avatars_select" on storage.objects
    for select using (bucket_id = 'avatars');

-- Built-in exercise catalog ---------------------------------------------------
-- Same fixed IDs as ExerciseCatalogSeed.kt, so a device that seeds its local
-- Room catalog and later syncs never creates duplicates (push is an upsert by id).

insert into public.exercises (id, name, muscle_group, equipment, is_custom) values
    ('a10c9b1e-1111-4a11-8000-000000000001', 'Panca piana', 'Petto', 'Bilanciere', false),
    ('a10c9b1e-1111-4a11-8000-000000000002', 'Panca inclinata', 'Petto', 'Bilanciere', false),
    ('a10c9b1e-1111-4a11-8000-000000000003', 'Croci ai cavi', 'Petto', 'Cavi', false),
    ('a10c9b1e-1111-4a11-8000-000000000004', 'Piegamenti', 'Petto', null, false),
    ('a10c9b1e-1111-4a11-8000-000000000005', 'Trazioni alla lat machine', 'Dorso', 'Lat machine', false),
    ('a10c9b1e-1111-4a11-8000-000000000006', 'Rematore con bilanciere', 'Dorso', 'Bilanciere', false),
    ('a10c9b1e-1111-4a11-8000-000000000007', 'Trazioni alla sbarra', 'Dorso', 'Sbarra', false),
    ('a10c9b1e-1111-4a11-8000-000000000008', 'Stacco da terra', 'Dorso', 'Bilanciere', false),
    ('a10c9b1e-1111-4a11-8000-000000000009', 'Squat', 'Gambe', 'Bilanciere', false),
    ('a10c9b1e-1111-4a11-8000-00000000000a', 'Leg press', 'Gambe', 'Macchina', false),
    ('a10c9b1e-1111-4a11-8000-00000000000b', 'Affondi', 'Gambe', 'Manubri', false),
    ('a10c9b1e-1111-4a11-8000-00000000000c', 'Leg curl', 'Gambe', 'Macchina', false),
    ('a10c9b1e-1111-4a11-8000-00000000000d', 'Leg extension', 'Gambe', 'Macchina', false),
    ('a10c9b1e-1111-4a11-8000-00000000000e', 'Polpacci in piedi', 'Gambe', 'Macchina', false),
    ('a10c9b1e-1111-4a11-8000-00000000000f', 'Military press', 'Spalle', 'Bilanciere', false),
    ('a10c9b1e-1111-4a11-8000-000000000010', 'Alzate laterali', 'Spalle', 'Manubri', false),
    ('a10c9b1e-1111-4a11-8000-000000000011', 'Alzate posteriori', 'Spalle', 'Manubri', false),
    ('a10c9b1e-1111-4a11-8000-000000000012', 'Curl bicipiti', 'Braccia', 'Manubri', false),
    ('a10c9b1e-1111-4a11-8000-000000000013', 'Curl a martello', 'Braccia', 'Manubri', false),
    ('a10c9b1e-1111-4a11-8000-000000000014', 'Push down ai cavi', 'Braccia', 'Cavi', false),
    ('a10c9b1e-1111-4a11-8000-000000000015', 'French press', 'Braccia', 'Bilanciere', false),
    ('a10c9b1e-1111-4a11-8000-000000000016', 'Plank', 'Core', null, false),
    ('a10c9b1e-1111-4a11-8000-000000000017', 'Crunch', 'Core', null, false),
    ('a10c9b1e-1111-4a11-8000-000000000018', 'Russian twist', 'Core', 'Disco', false)
on conflict (id) do nothing;
