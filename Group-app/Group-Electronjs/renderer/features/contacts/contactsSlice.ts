import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { orgAPI } from '../../services/api/apiClient';
import { OrgTreeNode, DepartmentInfo } from '../../types';

interface ContactsState {
    orgTree: OrgTreeNode[];
    loading: boolean;
    error: string | null;
}

const initialState: ContactsState = {
    orgTree: [],
    loading: false,
    error: null,
};

function buildTree(dept: DepartmentInfo): OrgTreeNode {
    const childrenNodes = (dept.children || []).map(buildTree);
    const memberNodes = (dept.members || []).map(user => ({
        id: user.userId,
        name: user.username,
        type: 'USER' as const,
        parentId: dept.departmentId,
        children: [],
        userInfo: user
    }));

    const children: OrgTreeNode[] = [...childrenNodes, ...memberNodes];

    return {
        id: dept.departmentId,
        name: dept.name,
        type: 'DEPARTMENT',
        parentId: dept.parentId,
        children,
        departmentInfo: dept
    };
}

export const fetchOrgStructure = createAsyncThunk(
    'contacts/fetchOrgStructure',
    async () => {
        const response = await orgAPI.getStructure();
        if (response.data && response.data.data) {
            return [buildTree(response.data.data)];
        }
        return [];
    }
);

const contactsSlice = createSlice({
    name: 'contacts',
    initialState,
    reducers: {},
    extraReducers: (builder) => {
        builder
            .addCase(fetchOrgStructure.pending, (state) => {
                state.loading = true;
            })
            .addCase(fetchOrgStructure.fulfilled, (state, action) => {
                state.loading = false;
                state.orgTree = action.payload;
            })
            .addCase(fetchOrgStructure.rejected, (state, action) => {
                state.loading = false;
                state.error = action.error.message || 'Failed to fetch org structure';
            });
    },
});

export default contactsSlice.reducer;
