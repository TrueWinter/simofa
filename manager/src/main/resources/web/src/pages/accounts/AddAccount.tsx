import { useActionData, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { notifications } from '@mantine/notifications';
import { Text } from '@mantine/core';
import { HttpResponse, post } from '../../util/api';
import { scrollToTop } from '../../util/scroll';
import Page from '../../components/Page';
import { requestBodyToJson } from '../../util/forms';
import AccountForm from '../../components/forms/AccountForm';

export function Component() {
  const navigate = useNavigate();
  const data = useActionData() as HttpResponse;

  useEffect(() => {
    if (data) {
      if (data.status === 200) {
        notifications.show({
          message: 'Account created'
        });
        navigate(`/accounts/${data.body.id}/edit`);
      } else {
        scrollToTop();
      }
    }
  }, [data]);

  return (
    <Page title="Add Account">
      {data && data.status !== 200 && <Text c="red">{data.body.title || 'An error occurred'}</Text>}
      <AccountForm />
    </Page>
  );
}

export async function action({ request }) {
  return post('/api/accounts', await requestBodyToJson(request));
}
