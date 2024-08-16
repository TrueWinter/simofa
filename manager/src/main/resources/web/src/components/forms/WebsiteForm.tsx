import { ActionIcon, Anchor, Box, Code, ComboboxItem, type ComboboxLikeRenderOptionInput,
  NumberInput, Select, Stack, Text, TextInput, Title } from '@mantine/core';
import { useState, useEffect, useRef } from 'react';
import { Form } from 'react-router-dom';
import { useForm } from '@mantine/form';
import { IconCheck, IconReload } from '@tabler/icons-react';
import type { GitCredential as Git, Website, DeployServer } from '../../types/java';
import { onSubmit, requestBodyToJson } from '../../util/forms';
import { get } from '../../util/api';
import FormSkeleton from '../FormSkeleton';
import ShortUuid from '../ShortUuid';
import LimitedLengthTextArea from '../LimitedLengthTextArea';
import TemplateModal, { type TemplateModalHandle } from '../TemplateModal';
import Error from '../Error';
import SubmitButton from '../SubmitButton';

const DEFAULT_SCRIPT = '#!/bin/bash\nset -e\n';

interface Props {
  website?: Website
}

function formatSelectOption(i: ComboboxLikeRenderOptionInput<ComboboxItem>) {
  return (
    <>
      {i.checked && <IconCheck size={18} opacity={0.6} />}
      {!i.option.value.includes('-') ? i.option.label : (
        <>
          <ShortUuid uuid={i.option.value} brackets /> {i.option.label}
        </>
      )}
    </>
  );
}

export default function WebsiteForm({ website }: Props) {
  const [servers, setServers] = useState([] as DeployServer[]);
  const [git, setGit] = useState([] as Git[]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [fetchingToken, setFetchingToken] = useState(false);
  const templateModalRef = useRef<TemplateModalHandle>(null);

  function validateCommand(v: string) {
    const CommandError = <>Commands must start with <Code>#!/bin/bash</Code></>;
    return !v.startsWith('#!/bin/bash') ? CommandError : null;
  }

  const form = useForm<Omit<Website, 'id'>>({
    initialValues: {
      name: website?.name || '',
      dockerImage: website?.dockerImage || '',
      memory: website?.memory || 256,
      cpu: website?.cpu || 1,
      gitUrl: website?.gitUrl || '',
      gitBranch: website?.gitBranch || '',
      gitCredentials: website?.gitCredentials || 'anonymous',
      buildCommand: website?.buildCommand || DEFAULT_SCRIPT,
      deployCommand: website?.deployCommand || DEFAULT_SCRIPT,
      deployFailedCommand: website?.deployFailedCommand || DEFAULT_SCRIPT,
      deployServer: website?.deployServer || '',
      deployToken: website?.deployToken
    },
    validate: {
      gitUrl: (v) => {
        try {
          // eslint-disable-next-line no-new
          new URL(v);
          return null;
        } catch (_) {
          return 'Invalid URL';
        }
      },
      buildCommand: validateCommand,
      deployCommand: validateCommand,
      deployFailedCommand: validateCommand
    },
    mode: 'uncontrolled',
    validateInputOnChange: true
  });

  async function setRandomDeployToken() {
    setFetchingToken(true);
    const resp = await get('/api/random?length=36');

    if (!resp.success) {
      setError(resp.body.title || 'An error occurred');
      setFetchingToken(false);
      return;
    }

    form.setFieldValue('deployToken', resp.body.random);
    setFetchingToken(false);
  }

  useEffect(() => {
    if (!form.getValues().deployToken) {
      setRandomDeployToken();
    }

    Promise.all([
      get<DeployServer[]>('/api/deploy-servers'),
      get<Git[]>('/api/git-credentials')
    ]).then((data) => {
      const deployResp = data[0];

      if (deployResp.success === false) {
        setError(deployResp.body.title || 'An error occurred');
        return;
      }
      setServers(deployResp.body);

      if (deployResp.body.length === 0) {
        setError('No deployment servers added yet');
        return;
      }

      const gitResp = data[1];
      if (gitResp.success === false) {
        setError(gitResp.body.title || 'An error occurred');
        return;
      }
      setGit(gitResp.body);
    }).catch((e) => {
      setError(`An error occurred while fetching deployment servers: ${e}`);
    }).finally(() => {
      setLoading(false);
    });
  }, []);

  return (
    <>
      <Title>{website ? 'Edit' : 'Add'} Website</Title>
      {/* eslint-disable-next-line no-nested-ternary */}
      {error ? <Error>{error}</Error> : (
        loading ? <FormSkeleton /> : (
          <>
            <Box mb="xs">
              <Anchor onClick={() => templateModalRef.current.open()}>Load template</Anchor>
            </Box>
            <TemplateModal form={form} ref={templateModalRef} />

            <Form method="POST" onSubmit={(e) => onSubmit(e, form)}>
              <Stack>
                <TextInput label="Website name" name="name" maxLength={40}
                  required key={form.key('name')} {...form.getInputProps('name')} />

                <TextInput label="Docker Image" name="dockerImage" required
                  description="It is recommended to use a specific tag instead of the latest image"
                  key={form.key('dockerImage')} {...form.getInputProps('dockerImage')} />

                <NumberInput label="Memory (MB)" name="memory" step={64}
                  min={64} max={99999} clampBehavior="strict" required
                  allowDecimal={false} key={form.key('memory')} {...form.getInputProps('memory')} />

                <NumberInput label="CPU Cores" name="cpu" step={0.25} required
                  description="Decimal values are supported" min={0.1} max={99}
                  clampBehavior="strict" key={form.key('cpu')} {...form.getInputProps('cpu')} />

                <TextInput label="Git URL" name="gitUrl" maxLength={255} required
                  key={form.key('gitUrl')} {...form.getInputProps('gitUrl')} />

                <TextInput label="Git Branch" name="gitBranch" maxLength={40} required
                  key={form.key('gitBranch')} {...form.getInputProps('gitBranch')} />

                <Select label="Git Credentials" name="gitCredentials" data={[{
                  label: 'anonymous',
                  value: 'anonymous'
                }, ...git.map((g) => ({
                  label: g.username,
                  value: g.id
                }))]} required renderOption={formatSelectOption}
                  key={form.key('gitCredentials')} {...form.getInputProps('gitCredentials')} />

                <LimitedLengthTextArea label="Build Command" name="buildCommand" maxLength={512}
                  rows={8} autosize required spellCheck={false} key={form.key('buildCommand')}
                  {...form.getInputProps('buildCommand')} />

                <LimitedLengthTextArea label="Deploy Command" name="deployCommand" maxLength={512}
                  rows={8} autosize required spellCheck={false} key={form.key('deployCommand')}
                  {...form.getInputProps('deployCommand')} />

                <LimitedLengthTextArea label="Deploy Failed Command" name="deployFailedCommand"
                  maxLength={512} rows={8} autosize required spellCheck={false}
                  key={form.key('deployFailedCommand')}
                  {...form.getInputProps('deployFailedCommand')} />

                <Select label="Deploy Server" name="deployServer" data={servers.map((s) => ({
                  label: s.name,
                  value: s.id
                }))} required renderOption={formatSelectOption}
                  key={form.key('deployServer')} {...form.getInputProps('deployServer')} />

                <Box>
                  <TextInput label="Deploy Token" name="deployToken" maxLength={36} required
                    readOnly rightSection={(
                      <ActionIcon loading={fetchingToken} onClick={setRandomDeployToken}>
                        <IconReload />
                      </ActionIcon>
                    )} description="Website must be saved for changes to deploy token to apply"
                    key={form.key('deployToken')} {...form.getInputProps('deployToken')} />
                  <Text size="sm" c="dimmed">{website ? (
                    <>
                      You can send a POST request to <Anchor href={`${location.protocol}//` +
                        `${location.host}/public-api/deploy-hook` +
                        `?website=${website.id}&token=${form.getValues().deployToken}`}
                        onClick={(e) => e.preventDefault()}>the deploy hook</Anchor> to trigger
                      a build. This token is also used as the secret for
                      the <Anchor href={`${location.protocol}//${location.host}/public-api/` +
                      `deploy/website/${website.id}/github`}>GitHub webhook endpoint</Anchor>.
                    </>
                  ) : 'Save website to view deploy hook URL'}
                  </Text>
                </Box>
              </Stack>

              <SubmitButton form={form}>{website ? 'Edit' : 'Add'} Website</SubmitButton>
            </Form>
          </>
        )
      )}
    </>
  );
}

export async function getBody(request: Request) {
  const body = await requestBodyToJson(request);
  if (body.gitCredentials === 'anonymous') {
    body.gitCredentials = null;
  }

  return body;
}
