export function addDraftMember(selectedMembers, user) {
  if (!user?.username || selectedMembers.some((item) => item.username === user.username)) {
    return selectedMembers;
  }
  return [...selectedMembers, user];
}

export function removeDraftMember(selectedMembers, username) {
  return selectedMembers.filter((item) => item.username !== username);
}

export function filterSelectableUsers(users, selectedMembers, currentUsername) {
  const selected = new Set(selectedMembers.map((item) => item.username));
  return users.filter((user) => user.username && user.username !== currentUsername && !selected.has(user.username));
}
