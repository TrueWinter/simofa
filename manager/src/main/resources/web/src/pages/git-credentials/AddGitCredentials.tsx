import { useActionData, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { notifications } from '@mantine/notifications';
import { Text } from '@mantine/core';
import { HttpResponse, post } from '../../util/api';
import { scrollToTop } from '../../util/scroll';
import Page from '../../components/Page';
import { requestBodyToJson } from '../../util/forms';
import GitCredentialsForm from '../../components/forms/GitCredentialsForm';

export function Component() {
  const navigate = useNavigate();
  const data = useActionData() as HttpResponse;

  useEffect(() => {
    if (data) {
      if (data.status === 200) {
        notifications.show({
          message: 'Git credentials created'
        });
        navigate(`/git-credentials/${data.body.id}/edit`);
      } else {
        scrollToTop();
      }
    }
  }, [data]);

  return (
    <Page title="Add Git Credentials">
      {data && data.status !== 200 && <Text c="red">{data.body.title || 'An error occurred'}</Text>}
      <GitCredentialsForm />
    </Page>
  );
}

export async function action({ request }) {
  return post('/api/git-credentials', await requestBodyToJson(request));
}
