import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { getWindowBreadcrumb } from '../../actions/MenuActions';
import MenuOverlayItem from './MenuOverlayItem';
import classnames from 'classnames';

class MenuOverlayContainer extends Component {
  constructor(props) {
    super(props);
  }

  handleClick = () => {
    const { dispatch, handleRedirect, elementId } = this.props;
    handleRedirect(elementId);
    dispatch(getWindowBreadcrumb(elementId));
  };

  render() {
    const {
      children,
      elementId,
      caption,
      type,
      handleClickOnFolder,
      handleRedirect,
      handleNewRedirect,
      handlePath,
      printChildren,
      deep,
      back,
      handleMenuOverlay,
      openModal,
      showBookmarks,
      onUpdateData,
      transparentBookmarks,
      onKeyDown,
      indexOrder,
      menuType,
      levelType,
    } = this.props;

    return (
      <div
        tabIndex={0}
        onKeyDown={onKeyDown}
        className={classnames('menu-overlay-node-container js-menu-container', {
          'mt-0': indexOrder === 1,
          'menu-overlay-node-spaced': deep,
          'menu-overlay-expanded-link-spaced js-menu-main-container': !deep,
          'menu-overlay-sitemap-col-2': menuType === 'sitemap', // we apply this only for the sitemap
        })}
      >
        {type === 'group' && !deep && menuType === 'sitemap' && (
          <div className="menu-overlay-header-main sitemap-box-header">
            {caption}
          </div>
        )}

        {type === 'group' && deep && (
          <span
            className={classnames('menu-overlay-heade', {
              'menu-overlay-header-spaced': !printChildren,
              'sitemap-level-one': menuType === 'sitemapLevelOne',
            })}
          >
            {caption}
          </span>
        )}

        {type !== 'group' && (
          <MenuOverlayItem
            printChildren={false}
            {...{
              showBookmarks,
              openModal,
              handlePath,
              back,
              type,
              caption,
              elementId,
              onUpdateData,
              transparentBookmarks,
              handleMenuOverlay,
              handleNewRedirect,
              handleRedirect,
              handleClickOnFolder,
            }}
          />
        )}

        {children &&
          children.length > 0 &&
          children.map((subitem, subindex) =>
            subitem.children && printChildren ? (
              <MenuOverlayContainer
                key={subindex}
                printChildren={true}
                deep={true}
                menuType={
                  levelType === 'navigationTree' ? 'sitemapLevelOne' : ''
                }
                {...subitem}
                {...{
                  showBookmarks,
                  openModal,
                  onUpdateData,
                  transparentBookmarks,
                  handleNewRedirect,
                  handleRedirect,
                  handleClickOnFolder,
                }}
              />
            ) : (
              <MenuOverlayItem
                key={subindex}
                {...subitem}
                {...{
                  showBookmarks,
                  openModal,
                  back,
                  printChildren,
                  handlePath,
                  onUpdateData,
                  transparentBookmarks,
                  handleMenuOverlay,
                  handleNewRedirect,
                  handleRedirect,
                  handleClickOnFolder,
                }}
              />
            )
          )}
      </div>
    );
  }
}

MenuOverlayContainer.propTypes = {
  dispatch: PropTypes.func,
  handleRedirect: PropTypes.func,
  handleClick: PropTypes.func,
  handleClickOnFolder: PropTypes.func,
  handleMenuOverlay: PropTypes.func,
  handleNewRedirect: PropTypes.func,
  handlePath: PropTypes.func,
  elementId: PropTypes.any,
  caption: PropTypes.string,
  type: PropTypes.string,
  children: PropTypes.any,
  printChildren: PropTypes.any,
  deep: PropTypes.any,
  back: PropTypes.any,
  openModal: PropTypes.func,
  showBookmarks: PropTypes.bool,
  onUpdateData: PropTypes.func,
  transparentBookmarks: PropTypes.bool,
  onKeyDown: PropTypes.func,
  indexOrder: PropTypes.number,
  menuType: PropTypes.string,
  levelType: PropTypes.string, // pass this to be able to differentiate between sitemap levels and breadcrumbs/quick menu listing
};

export default connect()(MenuOverlayContainer);
