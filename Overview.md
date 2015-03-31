# A-3 Overview #

## Introduction ##

A-3 is a Java framework that aims to help and simplify the implementation of self-management capabilities in highly dynamic and fully decentralised distributed applications.

## Problem Specification ##
<p>
Typically in enterprise software systems a single well-defined entity or a set of collaborating entities are in charge of providing self-management properties. This is possible because enterprise environments are often characterised by a well known and controlled communicative and informational structure.<br>
Instead scenarios with an high degree of dynamism and decentralisation make impossible a centralised approach to system management due to the lack of stable and well-defined entities to rely on.<br>
</p>

<p>
The role of A-3 is to address these kind of environments providing the tools to:<br>
<ul>
<li>build a self-organising control structure to get rid of dynamism and to build an accurate view of the running system</li>
<li>inject in running entities behavioural changes in order to adapt the system to environmental modification</li>
</ul>
</p>

A-3 provides the middleware to implement in a totally decentralised environment  a Monitor-Analyse-Plan-Execute control cycle which is at the heart of a self-managing software system.

## Architecture ##
<p>
In a decentralised component-based system, self-managing behaviour cannot be limited to single components, but each must react and change based on the overall picture. Only trough adaptation at component-level the system can reach a global self-managing behaviour.<br>
Therefore, ideally, each component should gather enough and up-to-date information of the system configuration as well as how the other components are behaving.<br>
This is obviously difficult if the system is highly dynamic, and components are continuously entering and exiting the system.<br>
</p>

<p>
In such a scenario we can't rely on a centralised supervision component and have all the other components send to it updated information regarding their behaviour. We are forced to adopt an entirely decentralised solution. In this case each component would need to communicate with all the others before being able to make a sound decision regarding what to do. This avoids having a single point of failure, but as soon as the number of nodes increase we have to face scalability issues since the messages exchanged will grow exponentially.<br>
</p>

<p>
To get rid of these problems A-3 model introduces the notion of group.<br>
A group represents a logical set of components that are brought together so that they can be supervised by a group leader.<br>
A group leader is a regular component that has been dynamically elected to leader status. IT is responsible for gathering information about the components in its group, and for sharing its knowledge with other group leaders thanks to an exclusive group called the leader-group. See the figure:<br>
</p>

<p>
<img src='https://a-3.googlecode.com/svn/wiki/images/GroupHierarchy.png' />
</p>

<p>
Groups use events to maintain a clear vision of the components they contain. This allows us to react to the addition or removal of a component, or the failure of a group leader. Moreover, when a leader fail we can promote a supervised component making it the new leader.<br>
Scalability issues are addressed by the construction of a group hierarchy (as shown in Figure 1). The hierarchy to allow group leaders to exchange information about their respective groups enabling them to build a global view of the without the need to interact with every single component in the system.<br>
</p>

<p>
A key aspect of our model is that the actual number of groups being used evolves together with the system at runtime. This is made possible through advanced group management features for dynamically creating or removing groups, and for migrating components from one group to another.<br>
</p>

<p>
Our model is implemented through a layered architecture as shown in the following figure:<br>
<img src='https://a-3.googlecode.com/svn/wiki/images/LayeredArchitecture.png' />
</p>

### Discovery Service ###
<p>
The Discovery Service is used by group members (leaders and supervised components) to obtain information about the current state of the system. It is implemented on REDS (REconfigurable Dispatching System) publish/subscribe subsystem and ideally can be used to build a totally decentralised discovery service layered on a peer-to-peer network structure.<br>
For the purpose of hierarchical group-based supervision structure the discovery service is used<br>
<ul><li>by new components to search for groups they want to belong to<br>
</li><li>by leaders to gather information about already existing groups (for instance to migrate member from one group to another in order to balance group size).<br>
</p></li></ul>

### Group Management ###
<p>
The group management layer is used to manage the number of groups and their configurations at runtime, and it has been implemented using two different Group Communication Framework:<br>
<ul><li>JGroups<br>
</li><li>Appia<br>
</p>
<p>
Despite the specific group communication framework used, the layer provides means to create and delete groups, as well as ways for components to join and leave a group. In practice it’s main goal is to provide reliable communication between the components of a single group. Hence, it provides both member-to-member and member-to-group communication. However, it also solves membership issues by providing an event-based membership management system. In fact, components can be notified when there are other components that are entering or leaving a group. It also provides means to detect and react to the crash of group members (leader included).<br>
</p></li></ul>


### Component Manager ###
<p>
The Component Manager orchestrates all the underlying layers and exposes the interfaces to control and manage the component. In the direction of a runtime behavioural adaptation the Component Manager provides the means to inject code into the component at runtime. For instance this facility allow group leader to inject code in the supervised components in response to environmental changes (i.e. network congestion or resources availability).<br>
</p>

## Links ##
<p>
REDS: <a href='http://zeus.ws.dei.polimi.it/reds'>http://zeus.ws.dei.polimi.it/reds</a>
<br />
JGroups: <a href='http://www.jgroups.org'>http://www.jgroups.org</a>
<br />
Appia: <a href='http://appia.di.fc.ul.pt'>http://appia.di.fc.ul.pt</a>
<br />
</p>