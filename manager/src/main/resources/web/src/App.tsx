import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { MantineProvider, type MantineProviderProps, Tooltip,
  createTheme, Table, Title, Select } from '@mantine/core';
import { NavigationProgress } from '@mantine/nprogress';
import { Notifications } from '@mantine/notifications';
import { ModalsProvider } from '@mantine/modals';
import { router } from './router';

import '@mantine/core/styles.css';
import '@mantine/nprogress/styles.css';
import '@mantine/notifications/styles.css';

const theme = createTheme({
  components: {
    Tooltip: Tooltip.extend({
      defaultProps: {
        withArrow: true,
        events: {
          hover: true,
          touch: true,
          focus: false
        }
      }
    }),
    Table: Table.extend({
      defaultProps: {
        highlightOnHover: true,
        fz: 'md'
      },
      styles: {
        td: {
          textAlign: 'center'
        },
        th: {
          textAlign: 'center'
        }
      }
    }),
    Title: Title.extend({
      defaultProps: {
        mb: 'xs'
      }
    }),
    Select: Select.extend({
      defaultProps: {
        searchable: true,
        nothingFoundMessage: 'No results'
      }
    })
  }
});

const props: MantineProviderProps = {
  defaultColorScheme: 'dark',
  withStaticClasses: false,
  theme
};

// eslint-disable-next-line no-undef
createRoot(document.getElementById('app')).render(
  <MantineProvider {...props}>
    <ModalsProvider>
      <NavigationProgress />
      <Notifications />
      <RouterProvider router={router} />
    </ModalsProvider>
  </MantineProvider>
);
