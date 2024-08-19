import { Box, Button, Code, Group, Text, TextInput } from '@mantine/core';
import { useState } from 'react';
import CopyButton from '../icons/CopyButton';

export interface GitHubAppConfig {
  url: string
  secret: string
  random: string
}

interface Props {
  config: GitHubAppConfig
  organization?: string
  disabled: boolean
}

const USER_BASE_URL = 'https://github.com/settings/apps/new';
const ORGANIZATION_BASE_URL = 'https://github.com/organizations/ORGANIZATION/settings/apps/new';

const createParams = (random: string, webhookUrl: string) => ({
  name: `simofa-app-${random}`,
  description: 'Simofa is a tool to help automate static website building and deployment',
  url: 'https://github.com/TrueWinter/simofa',
  public: 'false',
  webhook_active: 'true',
  webhook_url: webhookUrl,
  'events[]': 'push',
  contents: 'read'
});

export default function CreateAppButton({ config, organization, disabled }: Props) {
  const [webhook, setWebhook] = useState(config.url);

  const btnUrl = new URL(organization ?
    ORGANIZATION_BASE_URL.replace('ORGANIZATION', organization) : USER_BASE_URL);
  const btnUrlParams = createParams(config.random, webhook);
  Object.entries(btnUrlParams).forEach(([key, value]) => {
    btnUrl.searchParams.append(key, value);
  });

  return (
    <>
      <TextInput label="Webhook URL" value={webhook} required
        onChange={(e) => setWebhook(e.target.value)} />
      <Group gap="xs">
        <Text>Set the secret to <Code>{config.secret}</Code></Text>
        <CopyButton value={config.secret} />
      </Group>
      <Box>
        <Button component="a" href={!disabled ? btnUrl.href : undefined} target="_blank"
          disabled={disabled} w="100%">Create GitHub App</Button>
        <Text c="dimmed" size="sm">Remember to install the app after creating it</Text>
      </Box>
    </>
  );
}
