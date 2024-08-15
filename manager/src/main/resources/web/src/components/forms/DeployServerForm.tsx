import { Stack, TextInput, Title } from '@mantine/core';
import { Form } from 'react-router-dom';
import { useForm } from '@mantine/form';
import { onSubmit } from '../../util/forms';
import SubmitButton from '../SubmitButton';
import type { DeployServer } from '../../types/java';

interface Props {
  server?: DeployServer
}

export default function DeployServerForm({ server }: Props) {
  const form = useForm({
    initialValues: {
      name: server?.name || '',
      url: server?.url || '',
      key: server?.key || ''
    },
    validate: {
      url: (v) => {
        try {
          // eslint-disable-next-line no-new
          new URL(v);
          return null;
        } catch (e) {
          return 'Invalid URL';
        }
      }
    },
    mode: 'uncontrolled',
    validateInputOnChange: true
  });

  return (
    <>
      <Title>{server ? 'Edit' : 'Add'} Deploy Server</Title>
      <Form method="POST" onSubmit={(e) => onSubmit(e, form)}>
        <Stack>
          <TextInput label="Name" name="name" required maxLength={20}
            {...form.getInputProps('name')} key={form.key('name')} />
          <TextInput label="URL" name="url" required maxLength={256}
            {...form.getInputProps('url')} key={form.key('url')} />
          <TextInput label="Key" name="key" required maxLength={60}
            {...form.getInputProps('key')} key={form.key('key')} />
        </Stack>
        <SubmitButton form={form}>{server ? 'Edit' : 'Add'} Deploy Server</SubmitButton>
      </Form>
    </>
  );
}
