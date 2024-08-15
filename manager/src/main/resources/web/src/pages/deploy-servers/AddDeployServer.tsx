import { useActionData, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { notifications } from '@mantine/notifications';
import { Text } from '@mantine/core';
import { HttpResponse, post } from '../../util/api';
import { scrollToTop } from '../../util/scroll';
import Page from '../../components/Page';
import { requestBodyToJson } from '../../util/forms';
import DeployServerForm from '../../components/forms/DeployServerForm';

export function Component() {
  const navigate = useNavigate();
  const data = useActionData() as HttpResponse;

  useEffect(() => {
    if (data) {
      if (data.status === 200) {
        notifications.show({
          message: 'Deploy server created'
        });
        navigate(`/deploy-servers/${data.body.id}/edit`);
      } else {
        scrollToTop();
      }
    }
  }, [data]);

  return (
    <Page title="Add Deploy Server">
      {data && data.status !== 200 && <Text c="red">{data.body.title || 'An error occurred'}</Text>}
      <DeployServerForm />
    </Page>
  );
}

export async function action({ request }) {
  return post('/api/deploy-servers', await requestBodyToJson(request));
}
