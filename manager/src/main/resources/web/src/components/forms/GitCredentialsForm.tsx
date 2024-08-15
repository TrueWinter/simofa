import { PasswordInput, Stack, TextInput, Title } from '@mantine/core';
import { Form } from 'react-router-dom';
import { useForm } from '@mantine/form';
import { useDisclosure } from '@mantine/hooks';
import { onSubmit } from '../../util/forms';
import SubmitButton from '../SubmitButton';
import type { GitCredential } from '../../types/java';

interface Props {
  git?: GitCredential
}

export default function GitCredentialsForm({ git }: Props) {
  const [visible, { toggle }] = useDisclosure(false);
  const form = useForm({
    initialValues: {
      username: git?.username || '',
      password: '',
      confirmPassword: ''
    },
    validate: {
      confirmPassword: (v, values) => {
        if (values.password === '' && values.confirmPassword === '') return null;

        return v !== values.password ? 'Passwords must match' : null;
      }
    },
    mode: 'uncontrolled',
    validateInputOnChange: true
  });

  return (
    <>
      <Title>{git ? 'Edit' : 'Add'} Git Credentials</Title>
      <Form method="POST" onSubmit={(e) => onSubmit(e, form)}>
        <Stack>
          <TextInput label="Username" name="username" required maxLength={40}
            {...form.getInputProps('username')} key={form.key('username')} />
          <PasswordInput label="Password" name="password" required={!git} maxLength={80}
            description={git && 'Leave blank to leave unchanged'}
            visible={visible} onVisibilityChange={toggle}
            {...form.getInputProps('password')} key={form.key('password')} />
          <PasswordInput label="Confirm Password" name="confirmPassword" required={!git}
            maxLength={80} visible={visible} onVisibilityChange={toggle}
            {...form.getInputProps('confirmPassword')}
            key={form.key('confirmPassword')} />
        </Stack>
        <SubmitButton form={form}>{git ? 'Edit' : 'Add'} Git Credentials</SubmitButton>
      </Form>
    </>
  );
}
