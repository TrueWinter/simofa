import { Button, type ButtonProps } from '@mantine/core';
import { type UseFormReturnType } from '@mantine/form';

interface Props extends ButtonProps {
  form: UseFormReturnType<any>
}

export default function SubmitButton({ form, ...props }: Props) {
  const disabled = Object.keys(form.errors).length !== 0;

  return (
    <Button type="submit" mt="sm" disabled={disabled} {...props} />
  );
}
