import { IconPencil } from '@tabler/icons-react';
import { IconButtonLink } from '../Icon';

interface Props {
  to: string
}

export default function EditButton({ to }: Props) {
  return (
    <IconButtonLink label="Edit" to={to}>
      <IconPencil />
    </IconButtonLink>
  );
}
