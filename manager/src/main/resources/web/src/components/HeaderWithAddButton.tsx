import { IconPlus } from '@tabler/icons-react';
import HeaderWithButton, { HeaderWithButtonProps } from './HeaderWithButton';
import { IconButtonLink } from './Icon';

interface Props extends Omit<HeaderWithButtonProps, 'children'> {
  url: string
}

export default function HeaderWithAddButton(props: Props) {
  return (
    <HeaderWithButton {...props}>
      <IconButtonLink label="Add" to={props.url} size="lg">
        <IconPlus />
      </IconButtonLink>
    </HeaderWithButton>
  );
}
