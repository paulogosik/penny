alter table recurring_expenses
    add column last_generated_month date check (extract(day from last_generated_month) = 1);

alter table recurring_incomes
    add column last_generated_month date check (extract(day from last_generated_month) = 1);
