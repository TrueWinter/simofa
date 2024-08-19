import { Box, Loader, SegmentedControl, Stack, Text, TextInput, Title } from '@mantine/core';
import { useEffect, useState } from 'react';
import Page from '../../../components/Page';
import Error from '../../../components/Error';
import { get } from '../../../util/api';
import CreateAppButton, { GitHubAppConfig } from '../../../components/github-app/CreateAppButton';

export function Component() {
  const [config, setConfig] = useState<GitHubAppConfig>(null);
  const [error, setError] = useState(null);
  const [type, setType] = useState(null);
  const [organization, setOrganization] = useState('');

  useEffect(() => {
    get<GitHubAppConfig>('/api/config/github-app').then((resp) => {
      if (resp.success === false) {
        setError(resp.body.title || 'An error occurred');
        return;
      }

      setConfig(resp.body);
    });
  }, []);

  return (
    <Page title="GitHub App">
      <Title>GitHub App</Title>
      {error ? <Error>{error}</Error> : (
        (!error && !config) ? <Loader /> : (
          <Stack>
            <Box>
              <Text c="dimmed">GitHub Account Type</Text>
              <SegmentedControl onChange={setType} data={['User', 'Organization']} w="100%" />
            </Box>

            {type === 'Organization' && (
              <TextInput label="Organization Name" required value={organization}
                onChange={(e) => setOrganization(e.target.value)} />
            )}
            <CreateAppButton config={config} organization={type === 'Organization' && organization}
              disabled={type === 'Organization' && !organization} />
          </Stack>
        )
      )}
    </Page>
  );
}
