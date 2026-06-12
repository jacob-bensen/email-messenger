--run as postgres user on postgres db
create database email_messenger;

--run as postgres user on email_messenger db
create user email_messenger_app with password 'password'; --change this
GRANT ALL PRIVILEGES ON DATABASE email_messenger TO email_messenger_app;
GRANT CREATE, USAGE ON SCHEMA public TO email_messenger_app;
