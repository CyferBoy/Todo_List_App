-- Migration: Add lists/folders and Row Level Security
-- Run this in your Supabase SQL Editor

-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- Lists table
create table if not exists lists (
    id uuid primary key default uuid_generate_v4(),
    name text not null,
    type text not null check (type in ('public', 'private')),
    owner_id uuid references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);

-- Add list_id to tasks if not exists
do $$
begin
    if not exists (select 1 from information_schema.columns where table_name = 'tasks' and column_name = 'list_id') then
        alter table tasks add column list_id uuid references lists(id) on delete cascade;
    end if;
end $$;

-- Add deleted_at for soft deletes if not exists
do $$
begin
    if not exists (select 1 from information_schema.columns where table_name = 'tasks' and column_name = 'deleted_at') then
        alter table tasks add column deleted_at timestamptz;
    end if;
end $$;

-- Add deleted_at to lists if not exists
do $$
begin
    if not exists (select 1 from information_schema.columns where table_name = 'lists' and column_name = 'deleted_at') then
        alter table lists add column deleted_at timestamptz;
    end if;
end $$;

-- Create the default public list
insert into lists (id, name, type, owner_id)
values ('00000000-0000-0000-0000-000000000001', 'Public', 'public', null)
on conflict (id) do nothing;

-- Indexes
create index if not exists idx_tasks_list_id on tasks(list_id);
create index if not exists idx_tasks_updated_at on tasks(updated_at);
create index if not exists idx_tasks_deleted_at on tasks(deleted_at);
create index if not exists idx_tasks_completed on tasks(completed);
create index if not exists idx_tasks_due_date on tasks(due_date);
create index if not exists idx_lists_owner_id on lists(owner_id);
create index if not exists idx_lists_type on lists(type);
create index if not exists idx_lists_deleted_at on lists(deleted_at);

-- Enable RLS
alter table lists enable row level security;
alter table tasks enable row level security;

-- RLS Policies for LISTS

-- Public list (fixed id): SELECT for everyone
do $$
begin
    if not exists (select 1 from pg_policies where policyname = 'Public list readable by everyone' and tablename = 'lists') then
        create policy "Public list readable by everyone"
            on lists for select
            using (id = '00000000-0000-0000-0000-000000000001');
    end if;
end $$;

-- Public list: NO INSERT/UPDATE/DELETE for normal users
-- (blocked by absence of policies — RLS denies by default)

-- Private lists: owner can do everything
do $$
begin
    if not exists (select 1 from pg_policies where policyname = 'Private lists owner full access' and tablename = 'lists') then
        create policy "Private lists owner full access"
            on lists for all
            using (type = 'private' and owner_id = auth.uid())
            with check (type = 'private' and owner_id = auth.uid());
    end if;
end $$;

-- RLS Policies for TASKS

-- Tasks in the public list: full access for all authenticated users
do $$
begin
    if not exists (select 1 from pg_policies where policyname = 'Public list tasks full access' and tablename = 'tasks') then
        create policy "Public list tasks full access"
            on tasks for all
            using (list_id = '00000000-0000-0000-0000-000000000001')
            with check (list_id = '00000000-0000-0000-0000-000000000001');
    end if;
end $$;

-- Private tasks: only list owner can access
do $$
begin
    if not exists (select 1 from pg_policies where policyname = 'Private tasks owner access' and tablename = 'tasks') then
        create policy "Private tasks owner access"
            on tasks for all
            using (
                list_id in (
                    select l.id from lists l
                    where l.type = 'private' and l.owner_id = auth.uid()
                )
            )
            with check (
                list_id in (
                    select l.id from lists l
                    where l.type = 'private' and l.owner_id = auth.uid()
                )
            );
    end if;
end $$;

-- Enable Realtime for lists and tasks
-- Idempotent: only add if not already in publication
do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and tablename = 'lists'
    ) then
        alter publication supabase_realtime add table lists;
    end if;
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and tablename = 'tasks'
    ) then
        alter publication supabase_realtime add table tasks;
    end if;
end $$;

-- Update trigger for updated_at (idempotent)
create or replace function update_updated_at()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

drop trigger if exists update_lists_updated_at on lists;
create trigger update_lists_updated_at
    before update on lists
    for each row execute function update_updated_at();

drop trigger if exists update_tasks_updated_at on tasks;
create trigger update_tasks_updated_at
    before update on tasks
    for each row execute function update_updated_at();

-- Prevent modification of the public list (idempotent)
create or replace function prevent_public_list_modification()
returns trigger as $$
begin
    if (old.id = '00000000-0000-0000-0000-000000000001') then
        raise exception 'The global Public list cannot be modified';
    end if;
    return new;
end;
$$ language plpgsql;

drop trigger if exists block_public_list_update on lists;
create trigger block_public_list_update
    before update on lists
    for each row execute function prevent_public_list_modification();

drop trigger if exists block_public_list_delete on lists;
create trigger block_public_list_delete
    before delete on lists
    for each row execute function prevent_public_list_modification();
