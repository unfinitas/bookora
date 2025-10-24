export type UserId = string;

export type UserInfo = {
  Name?: string
};

export type User = { _id: UserId } & UserInfo;
