import { Pipe, PipeTransform } from '@angular/core';
import { OrganizationMember } from '../../core/models/organization-member.model';

@Pipe({ name: 'memberInitials' })
export class MemberInitialsPipe implements PipeTransform {
  transform(member: OrganizationMember): string {
    if (member.firstName && member.lastName) {
      return `${member.firstName[0]}${member.lastName[0]}`.toUpperCase();
    }
    if (member.firstName) {
      return member.firstName.slice(0, 2).toUpperCase();
    }
    return member.email.slice(0, 2).toUpperCase();
  }
}
