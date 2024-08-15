import { ActionIcon, Tooltip, createPolymorphicComponent,
  type ActionIconProps } from '@mantine/core';
import { forwardRef } from 'react';
import { Link, LinkProps } from 'react-router-dom';

export interface IconProps extends ActionIconProps {
  label: string
}

const Icon = createPolymorphicComponent<'button', IconProps>(
  forwardRef<HTMLButtonElement, IconProps>(({ label, ...others }, ref) => (
    <Tooltip label={label} openDelay={500}>
      <ActionIcon {...others} ref={ref} />
    </Tooltip>
  )));

export default Icon;

type IconButtonLinkUnion = IconProps & LinkProps
export type IconButtonLinkProps = Omit<IconButtonLinkUnion, 'component'>

export function IconButtonLink(props: IconButtonLinkProps) {
  return (
    <Icon component={Link} {...props} />
  );
}
