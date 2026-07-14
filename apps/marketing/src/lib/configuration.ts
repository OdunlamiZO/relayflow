function required(name: string): string {
  const value = process.env[name];

  if (!value || value.trim() === "") {
    throw new Error(`${name} is required but was not set.`);
  }

  return value;
}

function integerWithDefault(name: string, defaultValue: number): number {
  const value = process.env[name];

  if (!value || value.trim() === "") {
    return defaultValue;
  }

  return Number.parseInt(value, 10);
}

export const configuration = {
  databasePath: process.env.MARKETING_DB_PATH || "./data/marketing.sqlite",

  marketingBaseUrl: process.env.MARKETING_BASE_URL || "http://localhost:4000",

  paystack: {
    get secretKey() {
      return required("PAYSTACK_SECRET_KEY");
    },
    currency: process.env.PAYSTACK_CURRENCY || "NGN",
    get priceMinorUnits() {
      return Number.parseInt(required("PAYSTACK_PRICE_MINOR_UNITS"), 10);
    },
  },

  license: {
    termDays: integerWithDefault("LICENSE_TERM_DAYS", 365),
    get signingPrivateKeyBase64() {
      return required("LICENSE_SIGNING_PRIVATE_KEY_B64");
    },
  },

  email: {
    apiKey: process.env.RESEND_API_KEY || "",
    from: process.env.RESEND_FROM || "RelayFlow <licenses@relayflow.io>",
  },

  // The purchase flow can only complete once a human grants the buyer's
  // GitHub account access to the private image packages — required, not
  // optional-with-no-op like email, since a missed notification means the
  // order is stuck in awaiting_access_grant forever.
  telegram: {
    get botToken() {
      return required("TELEGRAM_BOT_TOKEN");
    },
    get chatId() {
      return required("TELEGRAM_CHAT_ID");
    },
  },

  rateLimit: {
    checkoutLimit: integerWithDefault("CHECKOUT_RATE_LIMIT", 5),
    checkoutWindowSeconds: integerWithDefault(
      "CHECKOUT_RATE_LIMIT_WINDOW_SECONDS",
      3600
    ),
  },
};
