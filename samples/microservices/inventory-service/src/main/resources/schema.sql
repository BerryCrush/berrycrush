create table if not exists inventory_items (
    id serial primary key,
    sku text,
    name text,
    quantity integer,
    reserved_quantity integer,
    unit_price numeric(10, 2)
);