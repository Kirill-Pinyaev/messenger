import { Code, ConnectError } from "@connectrpc/connect";

export function loginFeedback(err) {
  if (err instanceof ConnectError) {
    if (err.code === Code.NotFound) {
      return {
        message: "Такого ника не существует. Зарегистрируйтесь.",
        showRegisterPrompt: true,
      };
    }
    if (err.code === Code.Unauthenticated) {
      return {
        message: "Неправильный пароль.",
        showRegisterPrompt: false,
      };
    }
  }

  return {
    message: err instanceof Error ? err.message : String(err || "unknown error"),
    showRegisterPrompt: false,
  };
}
