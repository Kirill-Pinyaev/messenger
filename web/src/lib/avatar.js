export function initials(name) {
  return (name || "?")
    .split(" ")
    .map((word) => word[0] || "")
    .join("")
    .toUpperCase()
    .slice(0, 2) || "?";
}

export function avatarView(profile, fallbackColor, name) {
  if (profile?.avatarData) {
    return {
      kind: "image",
      src: profile.avatarData,
      alt: name || profile.username || "avatar",
    };
  }
  return {
    kind: "initials",
    color: profile?.avatarHex ? `#${profile.avatarHex}` : fallbackColor,
    text: initials(name),
  };
}
