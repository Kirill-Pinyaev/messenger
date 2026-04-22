import { createClient } from "@connectrpc/connect";
import { createGrpcWebTransport } from "@connectrpc/connect-web";

import { AuthService, MessageService, UserService } from "../gen/messenger/v1/messenger_connect.js";

export function buildGrpcWebOptions(baseUrl, interceptors = []) {
  return {
    baseUrl,
    interceptors,
    // grpc-web server wrapper expects protobuf framing, not JSON encoding.
    useBinaryFormat: true,
  };
}

export function createMessengerClients(baseUrl, authInterceptor) {
  const interceptors = authInterceptor ? [authInterceptor] : [];
  const transport = createGrpcWebTransport(buildGrpcWebOptions(baseUrl, interceptors));

  return {
    authClient: createClient(AuthService, transport),
    userClient: createClient(UserService, transport),
    messageClient: createClient(MessageService, transport),
  };
}
