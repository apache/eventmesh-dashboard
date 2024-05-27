import { NavMenuIdEnum } from '../../../routes/navigation/navigation.types'

export interface PublicState {
  navigation: { activeMenuId: NavMenuIdEnum; pinSubmenuIds: NavMenuIdEnum[] }
  seletedClusterId?: number
}
