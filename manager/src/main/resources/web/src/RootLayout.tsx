import { Link, Outlet, useLocation, useNavigation } from 'react-router-dom';
import { Paper, Container, Group, NavLink as MantineNavLink,
  Burger, Flex, Anchor, Divider } from '@mantine/core';
import { nprogress } from '@mantine/nprogress';
import { useDisclosure, useMediaQuery } from '@mantine/hooks';
import { useEffect, type ReactNode } from 'react';
import NavLink, { type NLink } from './NavLink';
import { usePathPattern } from './util/path';
import { getUserId } from './util/api';

const links: NLink[] = [{
  label: 'Websites',
  to: '/websites'
}, {
  label: 'Deploy Servers',
  to: '/deploy-servers'
}, {
  label: 'Accounts',
  to: '/accounts'
}, {
  label: 'Builds',
  to: '/builds'
}, {
  label: 'Git Credentials',
  to: '/git-credentials'
}];

interface MainContentProps {
  children: ReactNode
  hideOverflow: boolean
}

function MainContent(props: MainContentProps) {
  return (
    <Container py="xs" w="100%" h="100%" style={{
      display: 'flex',
      flexDirection: 'column',
      flex: 1,
      overflowY: props.hideOverflow ? 'hidden' : undefined
    }}>
      {props.children}
    </Container>
  );
}

export function Component() {
  const location = useLocation();
  const navigation = useNavigation();
  const [opened, { close, toggle }] = useDisclosure(false);
  const mobile = useMediaQuery('(max-width: 576px)');
  const path = usePathPattern();

  if (!mobile && !opened) {
    toggle();
  }

  const hideOverflowPaths = ['/websites/:websiteId/builds/:buildId/logs'];
  const hideOverflow = hideOverflowPaths.includes(path);

  useEffect(() => {
    close();
  }, [location]);

  setTimeout(() => {
    switch (navigation.state) {
      case 'idle':
        nprogress.complete();
        break;
      default:
        nprogress.start();
        break;
    }
  });

  const userId = getUserId();

  const rightNavLinks = (
    <>
      {userId && (
        <MantineNavLink
          w={!mobile && 'fit-content'}
          label="My Account"
          component={Link}
          to={`/accounts/${userId}/edit`}
        />
      )}
      <MantineNavLink
        w={!mobile && 'fit-content'}
        label="Logout"
        component={Link}
        to="/logout"
      />
    </>
  );

  const home = <Anchor component={Link} fw={700} size="lg" c="white" to="/">Simofa</Anchor>;

  // TODO: Find solution for button on edit/add website pages being hidden on mobile until the address bar is hidden
  return (
    <Flex direction="column" h={hideOverflow && '100dvh'} mih="100dvh" align="center">
      {!['/login', '/logout', '/refresh'].includes(location.pathname) && (
        <Paper p="md" w="100%" radius={0} bg="dark.9" c="white">
          <Group justify="space-between" hiddenFrom="md">
            {home}
            <Burger opened={opened} onClick={toggle} hidden={!mobile} />
          </Group>
          {opened && (
            <Flex justify="space-between" direction={mobile ? 'column' : 'row'}>
              <Group>
                {!mobile && home}
                <Flex gap="md" direction={mobile ? 'column' : 'row'}>
                  {links.map((e) => <NavLink key={e.to} link={e} />)}
                </Flex>
              </Group>
              <Divider hiddenFrom="md" />
              <Flex gap="sm" align="center" direction={mobile ? 'column' : 'row'}>
                {rightNavLinks}
              </Flex>
            </Flex>
          )}
        </Paper>
      )}
      <MainContent hideOverflow={hideOverflow}>
        <Outlet />
      </MainContent>
    </Flex>
  );
}
