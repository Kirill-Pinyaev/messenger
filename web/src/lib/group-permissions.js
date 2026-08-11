export function currentUserRole(conversation, username) {
  if (!conversation?.members?.length || !username) {
    return 0;
  }
  const member = conversation.members.find((item) => item.username === username);
  return member?.role || 0;
}

export function canManageGroupMembers(conversation, username) {
  return currentUserRole(conversation, username) !== 0;
}

export function canRemoveGroupMember(conversation, actorUsername, targetUsername) {
  if (!conversation?.members?.length || !actorUsername || !targetUsername || actorUsername === targetUsername) {
    return false;
  }

  const actor = conversation.members.find((item) => item.username === actorUsername);
  const target = conversation.members.find((item) => item.username === targetUsername);
  if (!actor || !target) {
    return false;
  }
  if (actor.role === 1) {
    return true;
  }
  if (target.role === 1) {
    return false;
  }
  return target.addedBy === actorUsername;
}

export function canTransferAdmin(conversation, username) {
  return currentUserRole(conversation, username) === 1;
}
