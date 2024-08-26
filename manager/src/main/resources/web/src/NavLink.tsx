import { NavLink as MantineLink } from '@mantine/core';
import { Link, useLocation } from 'react-router-dom';
import { useEffect, useRef } from 'react';
import { useClickOutside, useDisclosure, useId } from '@mantine/hooks';
import { useIsMobile } from './util/mobile';

export interface NLink {
  label: string
  to: string,
  links?: NLink[]
}

interface Props {
  link: NLink
}

export default function NavLink({ link }: Props) {
  const location = useLocation();
  const isActive = location.pathname.startsWith(link.to);
  const isMobile = useIsMobile();
  const [opened, { close, toggle }] = useDisclosure(false);
  const linkRef = useRef<HTMLAnchorElement>(null);
  const dropdownCoords = linkRef.current?.getBoundingClientRect();
  const id = useId();

  useClickOutside(close,
    ['mousedown', 'touchstart'],
    [linkRef.current, document.querySelector(`.${id}`)]);

  useEffect(() => {
    close();
  }, [location]);

  return link.links ? (
    <MantineLink w="fit-content" component={Link} label={link.label}
      to="#" active={isActive} opened={opened} onChange={toggle} ref={linkRef} classNames={{
        collapse: id
      }}
      styles={!isMobile && {
        collapse: {
          background: 'var(--mantine-color-dark-9)',
          padding: 12,
          position: 'absolute',
          left: dropdownCoords?.left,
          top: dropdownCoords ? (dropdownCoords.top + dropdownCoords.height + 12) : undefined,
          zIndex: 1000
        },
        children: {
          padding: 0
        }
      }}>
      {/* eslint-disable-next-line react/no-array-index-key */}
      {link.links.map((l, i) => <NavLink key={i} link={l} />)}
    </MantineLink>
  ) : (
    <MantineLink w="fit-content" component={Link} label={link.label}
      to={link.to} active={isActive} />
  );
}
