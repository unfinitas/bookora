export type AuthToken = string;
export type Username = string;
export type Password = string;
export type Email = string;

export type LocalAuth = {
  email: Email,
  password: Password
};

export type SignUpParams = {
  Email: Email
} & LocalAuth
