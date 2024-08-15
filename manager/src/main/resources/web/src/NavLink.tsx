import { NavLink as MantineLink } from '@mantine/core';
import { Link, useLocation } from 'react-router-dom';
import { useEffect } from 'react';

export interface NLink {
  label: string
  to: string
}

interface Props {
  link: NLink
}

export default function NavLink({ link }: Props) {
  const location = useLocation();
  const isActive = location.pathname.startsWith(link.to);

  useEffect(() => {
    close();
  }, [location]);

  return (
    <MantineLink w="fit-content" component={Link} label={link.label}
      to={link.to} active={isActive} />
  );
}
