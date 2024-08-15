/* eslint-disable react/no-array-index-key */
import { Skeleton, Table } from '@mantine/core';

interface Props {
  columns: number
  rows?: number
}

export default function TableSkeleton({ columns, rows = 5 }: Props) {
  return new Array(rows).fill(0).map((_, r) => (
    <Table.Tr key={r} style={{
      '--table-hover-color': 'initial'
    }}>
      <Table.Td colSpan={columns}>
        <Skeleton visible>
          Loading...
        </Skeleton>
      </Table.Td>
    </Table.Tr>
  ));
}
