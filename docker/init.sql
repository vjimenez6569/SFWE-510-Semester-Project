CREATE DATABASE ecommerce_product_dev;
CREATE DATABASE ecommerce_product_prod;
CREATE DATABASE ecommerce_order_dev;
CREATE DATABASE ecommerce_order_prod;
CREATE DATABASE ecommerce_customer_dev;
CREATE DATABASE ecommerce_customer_prod;

/* PRODUCT DEV */

\connect ecommerce_product_dev

CREATE TABLE IF NOT EXISTS public.products
(
    product_id text NOT NULL,
    product_name text NOT NULL,
    description text,
    price numeric(12,2) NOT NULL,
    quantity integer NOT NULL,
    category text,
    comment text,
    CONSTRAINT products_pkey PRIMARY KEY (product_id)
);

/* PRODUCT PROD */

\connect ecommerce_product_prod

CREATE TABLE IF NOT EXISTS public.products
(
    product_id text NOT NULL,
    product_name text NOT NULL,
    description text,
    price numeric(12,2) NOT NULL,
    quantity integer NOT NULL,
    category text,
    comment text,
    CONSTRAINT products_pkey PRIMARY KEY (product_id)
);

/* ORDER DEV */

\connect ecommerce_order_dev

CREATE TABLE IF NOT EXISTS public.customer_orders
(
    order_id text NOT NULL,
    customer_id text NOT NULL,
    customer_name text NOT NULL,
    order_date timestamp NOT NULL,
    order_status text NOT NULL,
    order_total numeric(12,2) NOT NULL,
    comment text,
    CONSTRAINT customer_orders_pkey PRIMARY KEY (order_id)
);

CREATE TABLE IF NOT EXISTS public.order_items
(
    order_item_id bigserial NOT NULL,
    order_id text NOT NULL,
    product_id text NOT NULL,
    product_name text NOT NULL,
    quantity integer NOT NULL,
    price numeric(12,2) NOT NULL,
    CONSTRAINT order_items_pkey PRIMARY KEY (order_item_id),
    CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id)
        REFERENCES public.customer_orders (order_id)
        ON DELETE CASCADE
);

/* ORDER PROD */

\connect ecommerce_order_prod

CREATE TABLE IF NOT EXISTS public.customer_orders
(
    order_id text NOT NULL,
    customer_id text NOT NULL,
    customer_name text NOT NULL,
    order_date timestamp NOT NULL,
    order_status text NOT NULL,
    order_total numeric(12,2) NOT NULL,
    comment text,
    CONSTRAINT customer_orders_pkey PRIMARY KEY (order_id)
);

CREATE TABLE IF NOT EXISTS public.order_items
(
    order_item_id bigserial NOT NULL,
    order_id text NOT NULL,
    product_id text NOT NULL,
    product_name text NOT NULL,
    quantity integer NOT NULL,
    price numeric(12,2) NOT NULL,
    CONSTRAINT order_items_pkey PRIMARY KEY (order_item_id),
    CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id)
        REFERENCES public.customer_orders (order_id)
        ON DELETE CASCADE
);

/* CUSTOMER DEV */

\connect ecommerce_customer_dev

CREATE TABLE IF NOT EXISTS public.customers
(
    customer_id text NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    email text NOT NULL,
    phone_number text,
    address text,
    comment text,
    CONSTRAINT customers_pkey PRIMARY KEY (customer_id),
    CONSTRAINT customers_email_unique UNIQUE (email)
);

/* CUSTOMER PROD */

\connect ecommerce_customer_prod

CREATE TABLE IF NOT EXISTS public.customers
(
    customer_id text NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    email text NOT NULL,
    phone_number text,
    address text,
    comment text,
    CONSTRAINT customers_pkey PRIMARY KEY (customer_id),
    CONSTRAINT customers_email_unique UNIQUE (email)
);