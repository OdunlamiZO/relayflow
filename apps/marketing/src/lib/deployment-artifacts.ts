import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { parse, stringify } from "yaml";

const CUSTOMER_PROFILE = "prod";

type ComposeService = {
  profiles?: string[];
  volumes?: string[];
};

type ComposeFile = {
  services: Record<string, ComposeService>;
  volumes?: Record<string, unknown>;
};

const DEPLOYMENT_ARTIFACTS_DIRECTORY = join(
  process.cwd(),
  "deployment-artifacts"
);
const REPOSITORY_ROOT = join(process.cwd(), "..", "..");

function readArtifact(
  fileName: string,
  repositoryRelativePath: string
): string {
  const dockerBuildPath = join(DEPLOYMENT_ARTIFACTS_DIRECTORY, fileName);

  if (existsSync(/* turbopackIgnore: true */ dockerBuildPath)) {
    return readFileSync(/* turbopackIgnore: true */ dockerBuildPath, "utf-8");
  }

  return readFileSync(
    /* turbopackIgnore: true */ join(REPOSITORY_ROOT, repositoryRelativePath),
    "utf-8"
  );
}

function extractCustomerProfile(raw: string): string {
  const parsed = parse(raw) as ComposeFile;

  const services = Object.fromEntries(
    Object.entries(parsed.services).filter(([, service]) =>
      service.profiles?.includes(CUSTOMER_PROFILE)
    )
  );

  if (Object.keys(services).length === 0) {
    throw new Error(
      `docker-compose.yml has no services in the "${CUSTOMER_PROFILE}" profile`
    );
  }

  const namedVolumes = new Set(
    Object.values(services)
      .flatMap((service) => service.volumes ?? [])
      .map((mount) => mount.split(":")[0])
      .filter((name) => !name.startsWith(".") && !name.startsWith("/"))
  );
  const volumes = Object.fromEntries(
    [...namedVolumes].map((name) => [name, parsed.volumes?.[name] ?? null])
  );

  return (
    "# RelayFlow self-hosted production stack.\n" +
    "# Run: docker compose --profile prod up -d\n\n" +
    stringify({ services, volumes })
  );
}

export const dockerComposeYml = extractCustomerProfile(
  readArtifact("docker-compose.yml", "docker-compose.yml")
);

export const envExample = readArtifact(".env.example", ".env.example");

export const caddyfile = readArtifact(
  "Caddyfile",
  "infra/docker/caddy/Caddyfile"
);
