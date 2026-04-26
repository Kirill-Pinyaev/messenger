export function identityStorageKey(username, deviceId) {
  const normalizedUsername = String(username || "").trim();
  const normalizedDeviceId = String(deviceId || "").trim();
  return normalizedDeviceId
    ? `messenger-e2ee-identity:${normalizedUsername}:${normalizedDeviceId}`
    : `messenger-e2ee-identity:${normalizedUsername}`;
}

export function hasUsableDirectIdentityMaterial(identity) {
  return !!(
    identity
    && identity.privateKey
    && identity.publicKeyBytes?.length > 0
    && identity.privateKeyJwk
    && identity.signedPrekey
    && identity.signedPrekey.privateKey
    && identity.signedPrekey.publicKeyBytes?.length > 0
    && identity.signedPrekey.privateKeyJwk
  );
}
