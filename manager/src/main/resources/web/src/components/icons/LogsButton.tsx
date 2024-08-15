import { IconLogs } from '@tabler/icons-react';
import { IconButtonLink } from '../Icon';

interface Props {
  to: string
}

export default function LogsButton({ to }: Props) {
  return (
    <IconButtonLink label="Logs" to={to}>
      <IconLogs />
    </IconButtonLink>
  );
}
