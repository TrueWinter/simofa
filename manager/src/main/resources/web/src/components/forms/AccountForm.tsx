import { PasswordInput, Stack, TextInput, Title } from '@mantine/core';
import { Form } from 'react-router-dom';
import { useForm } from '@mantine/form';
import { useDisclosure } from '@mantine/hooks';
import type { Account } from '../../types/java';
import { onSubmit } from '../../util/forms';
import SubmitButton from '../SubmitButton';

interface Props {
  account?: Account
}

export default function AccountForm({ account }: Props) {
  const [visible, { toggle }] = useDisclosure(false);
  const form = useForm({
    initialValues: {
      username: account?.username || '',
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
      <Title>{account ? 'Edit' : 'Add'} Account</Title>
      <Form method="POST" onSubmit={(e) => onSubmit(e, form)}>
        <Stack>
          <TextInput label="Username" name="username" required maxLength={20}
            {...form.getInputProps('username')} key={form.key('username')} />
          <PasswordInput label="Password" name="password" required={!account} maxLength={72}
            description={account && 'Leave blank to leave unchanged'}
            visible={visible} onVisibilityChange={toggle}
            {...form.getInputProps('password')} key={form.key('password')} />
          <PasswordInput label="Confirm Password" name="confirmPassword" required={!account}
            maxLength={72} visible={visible} onVisibilityChange={toggle}
            {...form.getInputProps('confirmPassword')}
            key={form.key('confirmPassword')} />
        </Stack>
        <SubmitButton form={form}>{account ? 'Edit' : 'Add'} Account</SubmitButton>
      </Form>
    </>
  );
}
