\connect ecommerce_product_dev

INSERT INTO public.products
(product_id, product_name, description, price, quantity, category, comment)
VALUES
('11111111-1111-1111-1111-111111111111', 'Gaming Mouse', 'Wireless gaming mouse', 49.99, 25, 'Electronics', 'I AM DEV'),
('22222222-2222-2222-2222-222222222222', 'Mechanical Keyboard', 'Mechanical gaming keyboard', 89.99, 15, 'Electronics', 'I AM DEV');


\connect ecommerce_customer_dev

INSERT INTO public.customers
(customer_id, first_name, last_name, email, phone_number, address, comment)
VALUES
(
    '33333333-3333-3333-3333-333333333333',
    'Sample',
    'Customer',
    'customer@example.com',
    '520-555-0100',
    'Tucson, AZ',
    'I AM DEV'
);


\connect ecommerce_order_dev

INSERT INTO public.customer_orders
(order_id, customer_id, customer_name, order_date, order_status, order_total, comment)
VALUES
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '33333333-3333-3333-3333-333333333333',
    'Sample Customer',
    CURRENT_TIMESTAMP,
    'NEW',
    49.99,
    'I AM DEV'
);

INSERT INTO public.order_items
(order_id, product_id, product_name, quantity, price)
VALUES
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111',
    'Gaming Mouse',
    1,
    49.99
);