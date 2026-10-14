create table users (
    id                uuid primary key default gen_random_uuid(),
    telegram_user_id  bigint not null unique,
    name              text,
    timezone          text not null default 'America/Sao_Paulo',
    created_at        timestamptz not null default now()
);

create table expense_categories (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid not null references users (id) on delete cascade,
    name            text not null,
    color           text,
    monthly_budget  numeric(12, 2) check (monthly_budget >= 0),
    is_fallback     boolean not null default false,
    archived_at     timestamptz,
    created_at      timestamptz not null default now(),
    check (not (is_fallback and archived_at is not null))
);

create unique index expense_categories_user_name_key
    on expense_categories (user_id, lower(name)) where archived_at is null;
create unique index expense_categories_user_fallback_key
    on expense_categories (user_id) where is_fallback;

create table income_categories (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid not null references users (id) on delete cascade,
    name         text not null,
    color        text,
    archived_at  timestamptz,
    created_at   timestamptz not null default now()
);

create unique index income_categories_user_name_key
    on income_categories (user_id, lower(name)) where archived_at is null;

create table credit_cards (
    id            uuid primary key default gen_random_uuid(),
    user_id       uuid not null references users (id) on delete cascade,
    name          text not null,
    closing_day   smallint not null check (closing_day between 1 and 31),
    due_day       smallint not null check (due_day between 1 and 31),
    credit_limit  numeric(12, 2) check (credit_limit >= 0),
    archived_at   timestamptz,
    created_at    timestamptz not null default now()
);

create unique index credit_cards_user_name_key
    on credit_cards (user_id, lower(name)) where archived_at is null;

create table installment_plans (
    id                  uuid primary key default gen_random_uuid(),
    user_id             uuid not null references users (id) on delete cascade,
    credit_card_id      uuid not null references credit_cards (id),
    category_id         uuid not null references expense_categories (id),
    description         text not null,
    total_amount        numeric(12, 2) not null check (total_amount > 0),
    installments_count  smallint not null check (installments_count between 2 and 99),
    purchased_on        date not null,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create index installment_plans_user_idx on installment_plans (user_id);

create table recurring_expenses (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid not null references users (id) on delete cascade,
    category_id     uuid not null references expense_categories (id),
    credit_card_id  uuid references credit_cards (id),
    description     text not null,
    amount          numeric(12, 2) not null check (amount > 0),
    day_of_month    smallint not null check (day_of_month between 1 and 31),
    starts_on       date not null,
    ends_on         date,
    active          boolean not null default true,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    check (ends_on is null or ends_on >= starts_on)
);

create index recurring_expenses_user_idx on recurring_expenses (user_id);

create table expenses (
    id                    uuid primary key default gen_random_uuid(),
    user_id               uuid not null references users (id) on delete cascade,
    category_id           uuid not null references expense_categories (id),
    amount                numeric(12, 2) not null check (amount > 0),
    description           text not null,
    occurred_on           date not null,
    source                text not null check (source in ('telegram_text', 'telegram_photo', 'panel', 'recurring')),
    raw_input             text,
    ai_model              text,
    receipt_path          text,
    credit_card_id        uuid references credit_cards (id),
    billing_month         date check (extract(day from billing_month) = 1),
    installment_plan_id   uuid references installment_plans (id) on delete cascade,
    installment_number    smallint check (installment_number >= 1),
    recurring_expense_id  uuid references recurring_expenses (id) on delete set null,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now(),
    check ((credit_card_id is null) = (billing_month is null)),
    check ((installment_plan_id is null) = (installment_number is null)),
    check (installment_plan_id is null or credit_card_id is not null)
);

create index expenses_user_occurred_on_idx on expenses (user_id, occurred_on);
create index expenses_user_billing_month_idx
    on expenses (user_id, billing_month) where billing_month is not null;
create index expenses_category_idx on expenses (category_id);
create unique index expenses_installment_key
    on expenses (installment_plan_id, installment_number) where installment_plan_id is not null;
create unique index expenses_recurring_month_key
    on expenses (recurring_expense_id, date_trunc('month', occurred_on::timestamp))
    where recurring_expense_id is not null;

create table recurring_incomes (
    id            uuid primary key default gen_random_uuid(),
    user_id       uuid not null references users (id) on delete cascade,
    category_id   uuid not null references income_categories (id),
    description   text not null,
    amount        numeric(12, 2) not null check (amount > 0),
    day_of_month  smallint not null check (day_of_month between 1 and 31),
    starts_on     date not null,
    ends_on       date,
    active        boolean not null default true,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    check (ends_on is null or ends_on >= starts_on)
);

create index recurring_incomes_user_idx on recurring_incomes (user_id);

create table incomes (
    id                   uuid primary key default gen_random_uuid(),
    user_id              uuid not null references users (id) on delete cascade,
    category_id          uuid not null references income_categories (id),
    amount               numeric(12, 2) not null check (amount > 0),
    description          text not null,
    received_on          date not null,
    source               text not null check (source in ('panel', 'recurring')),
    recurring_income_id  uuid references recurring_incomes (id) on delete set null,
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now()
);

create index incomes_user_received_on_idx on incomes (user_id, received_on);
create unique index incomes_recurring_month_key
    on incomes (recurring_income_id, date_trunc('month', received_on::timestamp))
    where recurring_income_id is not null;

create table goals (
    id             uuid primary key default gen_random_uuid(),
    user_id        uuid not null references users (id) on delete cascade,
    name           text not null,
    target_amount  numeric(12, 2) not null check (target_amount > 0),
    saved_amount   numeric(12, 2) not null default 0 check (saved_amount >= 0),
    target_date    date,
    achieved_at    timestamptz,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

create index goals_user_idx on goals (user_id);

alter table users enable row level security;
alter table expense_categories enable row level security;
alter table income_categories enable row level security;
alter table credit_cards enable row level security;
alter table installment_plans enable row level security;
alter table recurring_expenses enable row level security;
alter table expenses enable row level security;
alter table recurring_incomes enable row level security;
alter table incomes enable row level security;
alter table goals enable row level security;
