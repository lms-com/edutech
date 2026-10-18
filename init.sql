create database if not exists iam_db;
create database if not exists content_db;
create database if not exists media_db;
create database if not exists order_db;
create database if not exists finance_db;
create database if not exists enrollment_db;
create database if not exists worder_db;

grant all PRIVILEGES on *.* to 'root'@'%';
flush PRIVILEGES;