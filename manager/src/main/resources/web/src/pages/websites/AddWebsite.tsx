import { useActionData, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { notifications } from '@mantine/notifications';
import { Text } from '@mantine/core';
import WebsiteForm, { getBody } from '../../components/forms/WebsiteForm';
import { HttpResponse, post } from '../../util/api';
import { scrollToTop } from '../../util/scroll';
import Page from '../../components/Page';

export function Component() {
  const navigate = useNavigate();
  const data = useActionData() as HttpResponse;

  useEffect(() => {
    if (data) {
      if (data.status === 200) {
        notifications.show({
          message: 'Website created'
        });
        navigate(`/websites/${data.body.id}/edit`);
      } else {
        scrollToTop();
      }
    }
  }, [data]);

  return (
    <Page title="Add Website">
      {data && data.status !== 200 && <Text c="red">{data.body.title || 'An error occurred'}</Text>}
      <WebsiteForm />
    </Page>
  );
}

export async function action({ request }) {
  return post('/api/websites', await getBody(request));
}
