/*
 * GUIComponent.java
 * Created on 17. Juni 2006, 21:34
 *
 * This file is part of JAMS
 * Copyright (C) 2005 FSU Jena
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */

package jams.model;

import javax.swing.JPanel;

/**
 *
 * @author S. Kralisch
 */
public abstract class JAMSGUIComponent extends JAMSComponent implements GUIComponent {

    @Override
    public abstract JPanel getPanel();

}
