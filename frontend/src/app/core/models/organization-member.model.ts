export interface OrganizationMember {
  readonly id: string;
  readonly username: string;
  readonly email: string;
  readonly firstName?: string;
  readonly lastName?: string;
  readonly enabled: boolean;
}

export interface AddOrganizationMemberRequest {
  readonly email: string;
  readonly firstName?: string;
  readonly lastName?: string;
}
