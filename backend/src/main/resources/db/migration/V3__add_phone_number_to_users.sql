alter table users
    add column phone_number text unique check (phone_number ~ '^\+[1-9][0-9]{7,14}$');
