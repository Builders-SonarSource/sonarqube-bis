/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
// @flow
import { combineReducers } from 'redux';
import omit from 'lodash/omit';

export type Organization = {
  avatar?: string,
  canAdmin?: boolean,
  description?: string,
  isDefault?: boolean,
  key: string,
  name: string,
  url?: string
};

type ReceiveOrganizationsAction = {
  type: 'RECEIVE_ORGANIZATIONS',
  organizations: Array<Organization>
};

type UpdateOrganizationAction = {
  type: 'UPDATE_ORGANIZATION',
  key: string,
  changes: {}
};

type DeleteOrganizationAction = {
  type: 'DELETE_ORGANIZATION',
  key: string
};

type Action = ReceiveOrganizationsAction | UpdateOrganizationAction | DeleteOrganizationAction;

type ByKeyState = {
  [key: string]: Organization
};

type State = {
  byKey: ByKeyState
};

export const receiveOrganizations = (organizations: Array<Organization>): ReceiveOrganizationsAction => ({
  type: 'RECEIVE_ORGANIZATIONS',
  organizations
});

export const updateOrganization = (key: string, changes: {}): UpdateOrganizationAction => ({
  type: 'UPDATE_ORGANIZATION',
  key,
  changes
});

export const deleteOrganization = (key: string): DeleteOrganizationAction => ({
  type: 'DELETE_ORGANIZATION',
  key
});

const onReceiveOrganizations = (state: ByKeyState, action: ReceiveOrganizationsAction): ByKeyState => {
  const nextState = { ...state };
  action.organizations.forEach(organization => {
    nextState[organization.key] = { ...state[organization.key], ...organization };
  });
  return nextState;
};

const byKey = (state: ByKeyState = {}, action: Action) => {
  switch (action.type) {
    case 'RECEIVE_ORGANIZATIONS':
      return onReceiveOrganizations(state, action);
    case 'UPDATE_ORGANIZATION':
      return {
        ...state,
        [action.key]: {
          ...state[action.key],
          key: action.key,
          ...action.changes
        }
      };
    case 'DELETE_ORGANIZATION':
      return omit(state, action.key);
    default:
      return state;
  }
};

export default combineReducers({ byKey });

export const getOrganizationByKey = (state: State, key: string): Organization => (
    state.byKey[key]
);

export const areThereCustomOrganizations = (state: State): boolean => (
    Object.keys(state.byKey).length > 1
);
